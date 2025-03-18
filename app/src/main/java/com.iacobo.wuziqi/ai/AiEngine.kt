package com.iacobo.wuziqi.ai

import com.iacobo.wuziqi.data.GameState
import java.util.Random
import kotlin.math.abs
import kotlin.math.pow

/**
 * A threat-based Wuziqi (Gomoku) AI engine based on the principles described in:
 * - Victor Allis' Ph.D. thesis "Searching for solutions in games and artificial intelligence"
 * - The CodeCup 2020 winning bot "OOOOO"
 *
 * This AI uses pattern recognition and threat-sequence searches to play Wuziqi (Gomoku).
 */
class WuziqiAIEngine(private val random: Random = Random()) {

    companion object {
        // Player constants
        const val EMPTY = GameState.EMPTY
        const val PLAYER_ONE = GameState.PLAYER_ONE  // Human
        const val PLAYER_TWO = GameState.PLAYER_TWO  // Computer (AI)

        // Direction vectors for line detection
        val DIRECTIONS = arrayOf(
            Pair(1, 0),   // Horizontal
            Pair(0, 1),   // Vertical
            Pair(1, 1),   // Diagonal \
            Pair(1, -1)   // Diagonal /
        )
        
        // Edge distance constants
        const val MIN_EDGE_DISTANCE = 3
    }

    /**
     * Finds the best move for the current game state.
     */
    fun findBestMove(gameState: GameState): Pair<Int, Int>? {
        return when {
            gameState.boardSize == 3 && gameState.winCondition == 3 -> {
                findTicTacToeMove(gameState)
            }
            gameState.boardSize == 7 && gameState.winCondition == 4 -> {
                findConnect4Move(gameState)
            }
            else -> {
                findWuziqiMove(gameState)
            }
        }
    }

    /**
     * Core Wuziqi move finding logic
     */
    private fun findWuziqiMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val center = boardSize / 2
        val stoneCount = countStones(gameState)
        
        // Opening book moves
        if (stoneCount == 0) {
            // First move: play center
            return Pair(center, center)
        } else if (stoneCount == 1) {
            // Second move (responding to human's first move)
            if (gameState.board[center][center] == PLAYER_ONE) {
                // Human played center, respond with a book move
                val bookResponses = listOf(
                    Pair(center - 3, center),     // Traditional: 3 spaces up
                    Pair(center + 3, center),     // Traditional: 3 spaces down
                    Pair(center, center - 3),     // Traditional: 3 spaces left
                    Pair(center, center + 3),     // Traditional: 3 spaces right
                    Pair(center - 1, center - 1), // Modern: diagonal adjacent
                    Pair(center - 1, center + 1), // Modern: diagonal adjacent
                    Pair(center + 1, center - 1), // Modern: diagonal adjacent
                    Pair(center + 1, center + 1)  // Modern: diagonal adjacent
                )
                
                // Filter valid responses and pick one randomly
                val validResponses = bookResponses.filter { (r, c) ->
                    r in 0 until boardSize && c in 0 until boardSize && gameState.board[r][c] == EMPTY
                }
                
                if (validResponses.isNotEmpty()) {
                    return validResponses[random.nextInt(validResponses.size)]
                }
            } else {
                // Human didn't play center, we take center
                if (gameState.board[center][center] == EMPTY) {
                    return Pair(center, center)
                }
            }
        }
        
        // Look for winning moves and threats
        
        // 1. Check if we can win with the next move
        val winningMove = findWinningMove(gameState, PLAYER_TWO)
        if (winningMove != null) {
            return winningMove
        }
        
        // 2. Block opponent's winning move
        val blockWinningMove = findWinningMove(gameState, PLAYER_ONE)
        if (blockWinningMove != null) {
            return blockWinningMove
        }
        
        // 3. Check for creating or blocking forced-win situations
        val threatMove = findBestThreatMove(gameState)
        if (threatMove != null) {
            return threatMove
        }
        
        // 4. If no threats, use positional evaluation
        return findBestPositionalMove(gameState)
    }
    
    /**
     * Finds a move that would create a winning position (5 in a row)
     */
    private fun findWinningMove(gameState: GameState, player: Int): Pair<Int, Int>? {
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] == EMPTY) {
                    // Try placing a stone here
                    gameState.board[row][col] = player
                    
                    // Check if this creates a win
                    if (gameState.checkWin(row, col, player)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    
                    // Reset the position
                    gameState.board[row][col] = EMPTY
                }
            }
        }
        return null
    }
    
    /**
     * Finds the best move based on threat analysis.
     * This handles creating our own threats and blocking opponent's threats.
     */
    private fun findBestThreatMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        
        // Threat types in descending order of priority
        val threatTypes = listOf(
            ThreatType.OPEN_FOUR,      // Four with both ends open (guarantees a win)
            ThreatType.FOUR,           // Four with one end open (forces a defensive move)
            ThreatType.OPEN_THREE,     // Three with both ends open (forces a defensive move)
            ThreatType.THREE           // Three with one end open
        )
        
        // First, check if we can create a high-level threat
        for (threatType in threatTypes) {
            val createThreatMove = findCreateThreatMove(gameState, PLAYER_TWO, threatType)
            if (createThreatMove != null) {
                return createThreatMove
            }
        }
        
        // Then, check if we need to block opponent's threats, from highest to lowest priority
        for (threatType in threatTypes) {
            val blockThreatMove = findBlockThreatMove(gameState, PLAYER_ONE, threatType)
            if (blockThreatMove != null) {
                return blockThreatMove
            }
        }
        
        // If no immediate threats, try to create our own lower threats
        // First try to create an open three which is very strong
        val createOpenThreeMove = findCreateThreatMove(gameState, PLAYER_TWO, ThreatType.OPEN_THREE)
        if (createOpenThreeMove != null) {
            return createOpenThreeMove
        }
        
        // Then try to create a regular three
        val createThreeMove = findCreateThreatMove(gameState, PLAYER_TWO, ThreatType.THREE)
        if (createThreeMove != null) {
            return createThreeMove
        }
        
        // If we can't create any decent threats, try to create at least a two
        val createTwoMove = findCreateThreatMove(gameState, PLAYER_TWO, ThreatType.TWO)
        if (createTwoMove != null) {
            return createTwoMove
        }
        
        return null
    }
    
    /**
     * Finds a move that creates a specific threat for the given player
     */
    private fun findCreateThreatMove(gameState: GameState, player: Int, threatType: ThreatType): Pair<Int, Int>? {
        val opponent = if (player == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        val possibleMoves = mutableListOf<Pair<Int, Int>>()
        
        // Try each empty position
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] == EMPTY) {
                    // Place stone temporarily
                    gameState.board[row][col] = player
                    
                    // Check if this creates the desired threat
                    var threatCreated = false
                    for ((dRow, dCol) in DIRECTIONS) {
                        val lineInfo = analyzeLine(gameState, row, col, dRow, dCol, player)
                        if (lineHasThreat(lineInfo, threatType, player)) {
                            threatCreated = true
                            break
                        }
                    }
                    
                    // Remove stone
                    gameState.board[row][col] = EMPTY
                    
                    if (threatCreated) {
                        // If this is an early move, check if it's not too close to the edge
                        val stoneCount = countStones(gameState)
                        if (stoneCount < 10) {
                            val edgeDistance = minOf(row, col, gameState.boardSize - 1 - row, gameState.boardSize - 1 - col)
                            if (edgeDistance >= MIN_EDGE_DISTANCE) {
                                // Good position, away from edge
                                return Pair(row, col)
                            } else {
                                // Too close to edge, but save it as a possibility
                                possibleMoves.add(Pair(row, col))
                            }
                        } else {
                            // Later in the game, don't worry as much about edge distance
                            return Pair(row, col)
                        }
                    }
                }
            }
        }
        
        // If we couldn't find a good move away from the edge but have some possibilities
        if (possibleMoves.isNotEmpty()) {
            return possibleMoves[random.nextInt(possibleMoves.size)]
        }
        
        return null
    }
    
    /**
     * Finds a move that blocks a specific threat by the opponent
     */
    private fun findBlockThreatMove(gameState: GameState, opponent: Int, threatType: ThreatType): Pair<Int, Int>? {
        val player = if (opponent == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
        // First, detect if this threat exists on the board
        val threats = detectThreats(gameState, opponent, threatType)
        if (threats.isEmpty()) {
            return null // No threats of this type
        }
        
        // For each threat, determine the best blocking move
        val blockingMoves = mutableListOf<Pair<Int, Int>>()
        
        for (threat in threats) {
            // For open threats, we need to close one end
            if (threatType == ThreatType.OPEN_FOUR || threatType == ThreatType.OPEN_THREE) {
                // Block either end of the threat
                if (threat.openEnd1 != null && gameState.board[threat.openEnd1.first][threat.openEnd1.second] == EMPTY) {
                    blockingMoves.add(threat.openEnd1)
                }
                
                if (threat.openEnd2 != null && gameState.board[threat.openEnd2.first][threat.openEnd2.second] == EMPTY) {
                    blockingMoves.add(threat.openEnd2)
                }
            } else if (threatType == ThreatType.FOUR) {
                // For a simple four, we must block the open end
                if (threat.openEnd1 != null && gameState.board[threat.openEnd1.first][threat.openEnd1.second] == EMPTY) {
                    return threat.openEnd1 // Critical block, return immediately
                }
            } else if (threatType == ThreatType.THREE) {
                // For a simple three, blocking the open end is usually best
                if (threat.openEnd1 != null && gameState.board[threat.openEnd1.first][threat.openEnd1.second] == EMPTY) {
                    blockingMoves.add(threat.openEnd1)
                }
            }
        }
        
        // If we have blocking moves, choose the best one
        if (blockingMoves.isNotEmpty()) {
            // First check if any blocking move is also good for us
            for (move in blockingMoves) {
                gameState.board[move.first][move.second] = player
                
                var createsCounter = false
                for ((dRow, dCol) in DIRECTIONS) {
                    val lineInfo = analyzeLine(gameState, move.first, move.second, dRow, dCol, player)
                    // Check if this creates any good threat for us
                    if (lineHasThreat(lineInfo, ThreatType.OPEN_THREE, player) || 
                        lineHasThreat(lineInfo, ThreatType.FOUR, player)) {
                        createsCounter = true
                        break
                    }
                }
                
                gameState.board[move.first][move.second] = EMPTY
                
                if (createsCounter) {
                    return move // This move both blocks and creates a threat
                }
            }
            
            // If no move creates a counter-threat, just pick one
            return blockingMoves[random.nextInt(blockingMoves.size)]
        }
        
        return null
    }
    
    /**
     * Detects all threats of a specific type for a player
     */
    private fun detectThreats(gameState: GameState, player: Int, threatType: ThreatType): List<Threat> {
        val threats = mutableListOf<Threat>()
        
        // Check each piece on the board
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] == player) {
                    // Check in all directions
                    for ((dRow, dCol) in DIRECTIONS) {
                        val lineInfo = analyzeLine(gameState, row, col, dRow, dCol, player)
                        
                        // Check if this line has the specific threat type
                        if (lineHasThreat(lineInfo, threatType, player)) {
                            // Create threat object with relevant information
                            val threat = createThreatFromLine(lineInfo, threatType, row, col, dRow, dCol, gameState)
                            if (threat != null) {
                                threats.add(threat)
                            }
                        }
                    }
                }
            }
        }
        
        return threats
    }
    
    /**
     * Creates a Threat object from a line analysis
     */
    private fun createThreatFromLine(
        lineInfo: LineInfo,
        threatType: ThreatType,
        startRow: Int,
        startCol: Int,
        dRow: Int,
        dCol: Int,
        gameState: GameState
    ): Threat? {
        val boardSize = gameState.boardSize
        val player = lineInfo.player
        
        // Find the beginning of the player's stones in this line
        var consecutiveStart = -1
        var maxConsecutive = 0
        var currentConsecutive = 0
        
        for (i in lineInfo.line.indices) {
            if (lineInfo.line[i] == player) {
                if (currentConsecutive == 0) {
                    consecutiveStart = i
                }
                currentConsecutive++
                maxConsecutive = max(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }
        
        if (maxConsecutive < 2) {
            return null // Not enough consecutive stones
        }
        
        // Determine the ends of the threat
        var openEnd1: Pair<Int, Int>? = null
        var openEnd2: Pair<Int, Int>? = null
        
        // Check locations before and after the consecutive stones
        if (threatType == ThreatType.OPEN_FOUR || threatType == ThreatType.OPEN_THREE) {
            // Both ends must be open
            val before = consecutiveStart - 1
            if (before >= 0 && lineInfo.line[before] == EMPTY) {
                val r = startRow - (consecutiveStart - before) * dRow
                val c = startCol - (consecutiveStart - before) * dCol
                if (r in 0 until boardSize && c in 0 until boardSize) {
                    openEnd1 = Pair(r, c)
                }
            }
            
            val consecutiveEnd = consecutiveStart + maxConsecutive - 1
            val after = consecutiveEnd + 1
            if (after < lineInfo.line.size && lineInfo.line[after] == EMPTY) {
                val r = startRow + (after - consecutiveStart) * dRow
                val c = startCol + (after - consecutiveStart) * dCol
                if (r in 0 until boardSize && c in 0 until boardSize) {
                    openEnd2 = Pair(r, c)
                }
            }
        } else if (threatType == ThreatType.FOUR || threatType == ThreatType.THREE) {
            // Only one end needs to be open
            val before = consecutiveStart - 1
            val consecutiveEnd = consecutiveStart + maxConsecutive - 1
            val after = consecutiveEnd + 1
            
            if (before >= 0 && lineInfo.line[before] == EMPTY) {
                val r = startRow - (consecutiveStart - before) * dRow
                val c = startCol - (consecutiveStart - before) * dCol
                if (r in 0 until boardSize && c in 0 until boardSize) {
                    openEnd1 = Pair(r, c)
                }
            }
            
            if (openEnd1 == null && after < lineInfo.line.size && lineInfo.line[after] == EMPTY) {
                val r = startRow + (after - consecutiveStart) * dRow
                val c = startCol + (after - consecutiveStart) * dCol
                if (r in 0 until boardSize && c in 0 until boardSize) {
                    openEnd1 = Pair(r, c)
                }
            }
        }
        
        // Create the threat object
        return Threat(
            threatType = threatType,
            player = player,
            openEnd1 = openEnd1,
            openEnd2 = openEnd2
        )
    }
    
    /**
     * Checks if a line contains a specific threat type
     */
    private fun lineHasThreat(lineInfo: LineInfo, threatType: ThreatType, player: Int): Boolean {
        val line = lineInfo.line
        val consecutive = countConsecutive(line, player)
        val openEnds = countOpenEnds(line, player)
        
        return when (threatType) {
            ThreatType.OPEN_FOUR -> consecutive == 4 && openEnds == 2
            ThreatType.FOUR -> consecutive == 4 && openEnds == 1
            ThreatType.OPEN_THREE -> consecutive == 3 && openEnds == 2
            ThreatType.THREE -> consecutive == 3 && openEnds == 1
            ThreatType.TWO -> consecutive == 2 && openEnds >= 1
        }
    }
    
    /**
     * Counts the maximum consecutive pieces for a player in a line
     */
    private fun countConsecutive(line: List<Int>, player: Int): Int {
        var maxConsecutive = 0
        var currentConsecutive = 0
        
        for (cell in line) {
            if (cell == player) {
                currentConsecutive++
                maxConsecutive = max(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }
        
        return maxConsecutive
    }
    
    /**
     * Counts the number of open ends for a player's consecutive stones
     */
    private fun countOpenEnds(line: List<Int>, player: Int): Int {
        var openEnds = 0
        var inConsecutive = false
        
        // Check for an open end before the consecutive stones
        for (i in line.indices) {
            if (line[i] == player) {
                if (!inConsecutive && i > 0 && line[i-1] == EMPTY) {
                    openEnds++
                }
                inConsecutive = true
            } else if (inConsecutive) {
                // End of consecutive stones
                if (line[i] == EMPTY) {
                    openEnds++
                }
                break
            }
        }
        
        return openEnds
    }
    
    /**
     * Analyzes a line from a starting position in a specific direction
     */
    private fun analyzeLine(
        gameState: GameState,
        row: Int,
        col: Int,
        dRow: Int,
        dCol: Int,
        player: Int
    ): LineInfo {
        val boardSize = gameState.boardSize
        val line = mutableListOf<Int>()
        val positions = mutableListOf<Pair<Int, Int>?>()
        
        // Extract 11 positions centered on the current position (5 on each side)
        for (i in -5..5) {
            val r = row + i * dRow
            val c = col + i * dCol
            
            if (r in 0 until boardSize && c in 0 until boardSize) {
                line.add(gameState.board[r][c])
                positions.add(Pair(r, c))
            } else {
                // Treat off-board as occupied by opponent (blocked)
                line.add(if (player == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE)
                positions.add(null)
            }
        }
        
        return LineInfo(player, line, positions)
    }
    
    /**
     * Finds the best move based on positional evaluation
     */
    private fun findBestPositionalMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val center = boardSize / 2
        val stoneCount = countStones(gameState)
        
        // Early game strategy: focus on central area
        if (stoneCount < 10) {
            val candidates = mutableListOf<Pair<Pair<Int, Int>, Int>>() // (position, score)
            
            // Consider all empty positions that aren't too close to the edge
            for (row in MIN_EDGE_DISTANCE until boardSize - MIN_EDGE_DISTANCE) {
                for (col in MIN_EDGE_DISTANCE until boardSize - MIN_EDGE_DISTANCE) {
                    if (gameState.board[row][col] == EMPTY) {
                        // Score based on distance from center (closer is better)
                        val centerDistance = abs(row - center) + abs(col - center)
                        val positionScore = boardSize - centerDistance
                        
                        candidates.add(Pair(Pair(row, col), positionScore))
                    }
                }
            }
            
            // If we have candidates, choose one of the best ones
            if (candidates.isNotEmpty()) {
                candidates.sortByDescending { it.second } // Sort by score (descending)
                val bestCandidates = candidates.take(3) // Take top 3 positions
                return bestCandidates[random.nextInt(bestCandidates.size)].first
            }
        }
        
        // Mid-late game strategy: evaluate positions near existing stones
        val candidates = findCandidatesNearStones(gameState)
        if (candidates.isNotEmpty()) {
            val scoredCandidates = mutableListOf<Pair<Pair<Int, Int>, Int>>()
            
            for ((row, col) in candidates) {
                if (gameState.board[row][col] == EMPTY) {
                    // Evaluate position
                    val score = evaluatePosition(gameState, row, col)
                    scoredCandidates.add(Pair(Pair(row, col), score))
                }
            }
            
            // Sort by score and take best move
            if (scoredCandidates.isNotEmpty()) {
                scoredCandidates.sortByDescending { it.second }
                return scoredCandidates[0].first
            }
        }
        
        // Fallback: just find any empty spot, starting from center and working outward
        for (distance in 0 until boardSize) {
            for (dr in -distance..distance) {
                for (dc in -distance..distance) {
                    // Only check positions at exactly 'distance' from center
                    if (abs(dr) == distance || abs(dc) == distance) {
                        val r = center + dr
                        val c = center + dc
                        if (r in 0 until boardSize && c in 0 until boardSize && 
                            gameState.board[r][c] == EMPTY) {
                            return Pair(r, c)
                        }
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Finds candidate moves near existing stones
     */
    private fun findCandidatesNearStones(gameState: GameState): List<Pair<Int, Int>> {
        val boardSize = gameState.boardSize
        val candidates = mutableSetOf<Pair<Int, Int>>()
        
        // Check positions adjacent to existing stones
        val directions = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1
        )
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    // Check all adjacent positions
                    for ((dr, dc) in directions) {
                        val r = row + dr
                        val c = col + dc
                        
                        if (r in 0 until boardSize && c in 0 until boardSize && 
                            gameState.board[r][c] == EMPTY) {
                            candidates.add(Pair(r, c))
                        }
                    }
                }
            }
        }
        
        return candidates.toList()
    }
    
    /**
     * Evaluates a position for strategic value
     */
    private fun evaluatePosition(gameState: GameState, row: Int, col: Int): Int {
        val boardSize = gameState.boardSize
        val center = boardSize / 2
        var score = 0
        
        // Don't play too close to the edge in early game
        val stoneCount = countStones(gameState)
        if (stoneCount < 10) {
            val edgeDistance = minOf(row, col, boardSize - 1 - row, boardSize - 1 - col)
            if (edgeDistance < MIN_EDGE_DISTANCE) {
                return -1000 // Strong penalty for edge positions
            }
        }
        
        // Prefer positions closer to the center
        val centerDistance = abs(row - center) + abs(col - center)
        score -= centerDistance * 2
        
        // Place stone temporarily
        gameState.board[row][col] = PLAYER_TWO
        
        // Check potential for creating threats
        for ((dRow, dCol) in DIRECTIONS) {
            val lineInfo = analyzeLine(gameState, row, col, dRow, dCol, PLAYER_TWO)
            
            // Score based on potential threats
            if (lineHasThreat(lineInfo, ThreatType.OPEN_THREE, PLAYER_TWO)) score += 500
            else if (lineHasThreat(lineInfo, ThreatType.THREE, PLAYER_TWO)) score += 100
            else if (lineHasThreat(lineInfo, ThreatType.TWO, PLAYER_TWO)) score += 10
            
            // Score based on blocking opponent threats
            val opponentLineInfo = analyzeLine(gameState, row, col, dRow, dCol, PLAYER_ONE)
            if (lineHasThreat(opponentLineInfo, ThreatType.OPEN_THREE, PLAYER_ONE)) score += 400
            else if (lineHasThreat(opponentLineInfo, ThreatType.THREE, PLAYER_ONE)) score += 80
        }
        
        // Remove temporary stone
        gameState.board[row][col] = EMPTY
        
        return score
    }
    
    /**
     * Counts the total number of stones on the board
     */
    private fun countStones(gameState: GameState): Int {
        var count = 0
        for (row in 0 until gameState.boardSize) {
            for (col in 0 until gameState.boardSize) {
                if (gameState.board[row][col] != EMPTY) {
                    count++
                }
            }
        }
        return count
    }

    /**
     * Finds the best move for TicTacToe.
     */
    private fun findTicTacToeMove(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE

        // 1. Check for winning move
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    gameState.board[row][col] = computerPlayer
                    if (gameState.checkWin(row, col, computerPlayer)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    gameState.board[row][col] = EMPTY
                }
            }
        }

        // 2. Block opponent's winning move
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (gameState.isTileEmpty(row, col)) {
                    gameState.board[row][col] = humanPlayer
                    if (gameState.checkWin(row, col, humanPlayer)) {
                        gameState.board[row][col] = EMPTY
                        return Pair(row, col)
                    }
                    gameState.board[row][col] = EMPTY
                }
            }
        }

        // 3. Take center if available
        if (gameState.isTileEmpty(1, 1)) {
            return Pair(1, 1)
        }

        // 4. Take corners
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        for ((row, col) in corners) {
            if (gameState.isTileEmpty(row, col)) {
                return Pair(row, col)
            }
        }

        // 5. Take any available edge
        val edges = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))
        for ((row, col) in edges) {
            if (gameState.isTileEmpty(row, col)) {
                return Pair(row, col)
            }
        }

        return null
    }

    /**
     * Finds the bottom-most empty row in a column for Connect4.
     */
    private fun findBottomEmptyRow(gameState: GameState, col: Int): Int {
        // Connect4 is considered to be a 7x6 board with 4-in-a-row to win
        val maxRow = if (gameState.boardSize == 7 && gameState.winCondition == 4) {
            5 // 6 rows (0-5) for Connect4
        } else {
            gameState.boardSize - 1
        }

        for (row in maxRow downTo 0) {
            if (gameState.board[row][col] == EMPTY) {
                return row
            }
        }
        return -1 // Column is full
    }

    /**
     * Finds the best move for Connect4 using a specialized approach for vertical boards.
     */
    private fun findConnect4Move(gameState: GameState): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val computerPlayer = PLAYER_TWO
        val humanPlayer = PLAYER_ONE

        // 1. Check for immediate win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                gameState.board[row][col] = computerPlayer
                if (gameState.checkWin(row, col, computerPlayer)) {
                    gameState.board[row][col] = EMPTY
                    return Pair(row, col)
                }
                gameState.board[row][col] = EMPTY
            }
        }

        // 2. Block opponent's win
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                gameState.board[row][col] = humanPlayer
                if (gameState.checkWin(row, col, humanPlayer)) {
                    gameState.board[row][col] = EMPTY
                    return Pair(row, col)
                }
                gameState.board[row][col] = EMPTY
            }
        }
        
        // 3. Critical: Block open two threats on the bottom row
        val maxRow = if (gameState.boardSize == 7) 5 else gameState.boardSize - 1
        val result = findConnect4OpenTwoBlock(gameState, maxRow, humanPlayer)
        if (result != null) {
            return result
        }

        // 4. Look for Connect4-specific forced win setups
        val forcingMoveResult = findConnect4ForcingMove(gameState, computerPlayer)
        if (forcingMoveResult != null) {
            return forcingMoveResult
        }

        // 5. Block opponent's forcing moves
        val blockForcingMoveResult = findConnect4ForcingMove(gameState, humanPlayer)
        if (blockForcingMoveResult != null) {
            return blockForcingMoveResult
        }

        // 6. Evaluate positions with Connect4-specific heuristics
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null
        
        // Column preference: center > sides
        val centerCol = boardSize / 2
        val colValues = (0 until boardSize).sortedBy { abs(it - centerCol) }
        
        for (col in colValues) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Place a temporary stone
                gameState.board[row][col] = computerPlayer
                
                // Evaluate this position using Connect4-specific evaluation
                val score = evaluateConnect4Position(gameState, row, col, computerPlayer)
                
                // Remove temporary stone
                gameState.board[row][col] = EMPTY
                
                if (score > bestScore) {
                    bestScore = score
                    bestMove = Pair(row, col)
                }
            }
        }
        
        return bestMove
    }
    
    /**
     * Specifically handles blocking open two threats on the bottom row in Connect4.
     * For example: "--rr--" needs to be blocked at position 3 (0-indexed, between the two 'r's).
     */
    private fun findConnect4OpenTwoBlock(gameState: GameState, bottomRow: Int, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        
        // Check horizontal open two threats on bottom row
        for (col in 0 until boardSize - 3) {
            // Look for two adjacent opponent pieces with spaces on either side
            if (gameState.board[bottomRow][col] == EMPTY &&
                gameState.board[bottomRow][col+1] == playerValue &&
                gameState.board[bottomRow][col+2] == playerValue &&
                col+3 < boardSize && gameState.board[bottomRow][col+3] == EMPTY) {
                
                // Block by playing immediately before or after the pair
                // Prefer blocking before if both are available
                if (gameState.isTileEmpty(bottomRow, col)) {
                    return Pair(bottomRow, col)
                } else if (col+3 < boardSize && gameState.isTileEmpty(bottomRow, col+3)) {
                    return Pair(bottomRow, col+3)
                }
            }
        }
        
        // Check for pattern with a gap: "r-r"
        for (col in 0 until boardSize - 2) {
            if (gameState.board[bottomRow][col] == playerValue &&
                gameState.board[bottomRow][col+1] == EMPTY &&
                gameState.board[bottomRow][col+2] == playerValue) {
                
                // Block the gap
                return Pair(bottomRow, col+1)
            }
        }
        
        // Also check for patterns like "-rr-" where we need to block one of the ends
        for (col in 1 until boardSize - 2) {
            if (gameState.board[bottomRow][col-1] == EMPTY &&
                gameState.board[bottomRow][col] == playerValue &&
                gameState.board[bottomRow][col+1] == playerValue &&
                gameState.board[bottomRow][col+2] == EMPTY) {
                
                // Block one of the ends, prioritize the center side
                val centerCol = boardSize / 2
                if (Math.abs(col-1 - centerCol) < Math.abs(col+2 - centerCol)) {
                    return Pair(bottomRow, col-1)
                } else {
                    return Pair(bottomRow, col+2)
                }
            }
        }
        
        return null
    }
    
    /**
     * Finds forcing moves specific to Connect4 strategy.
     * Looks for potential "trap" setups where playing one move forces a win.
     */
    private fun findConnect4ForcingMove(gameState: GameState, playerValue: Int): Pair<Int, Int>? {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        
        // Check for potential double-threat setups (two winning moves in a single turn)
        for (col in 0 until boardSize) {
            val row = findBottomEmptyRow(gameState, col)
            if (row != -1) {
                // Place a temporary stone
                gameState.board[row][col] = playerValue
                
                // Count how many winning moves would be available after this move
                var winningCols = 0
                var firstWinningCol = -1
                
                // Check if this creates a potential winning move in the next turn
                for (nextCol in 0 until boardSize) {
                    val nextRow = findBottomEmptyRow(gameState, nextCol)
                    if (nextRow != -1) {
                        gameState.board[nextRow][nextCol] = playerValue
                        if (gameState.checkWin(nextRow, nextCol, playerValue)) {
                            winningCols++
                            if (firstWinningCol == -1) {
                                firstWinningCol = nextCol
                            }
                        }
                        gameState.board[nextRow][nextCol] = EMPTY
                    }
                }
                
                // Remove the temporary stone
                gameState.board[row][col] = EMPTY
                
                // If this move creates two or more winning possibilities, it's a forcing move
                if (winningCols >= 2) {
                    return Pair(row, col)
                }
            }
        }
        
        return null
    }
    
    /**
     * Evaluates a Connect4 position with specific heuristics for the game.
     */
    private fun evaluateConnect4Position(gameState: GameState, row: Int, col: Int, playerValue: Int): Int {
        val boardSize = gameState.boardSize
        val opponent = if (playerValue == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
        var score = 0
        
        // Prioritize center columns
        val centerScore = boardSize - 2 * abs(col - boardSize / 2)
        score += centerScore * 3
        
        // Count potential winning lines through this position
        val directions = arrayOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),   // Vertical
            Pair(1, 1),   // Diagonal \
            Pair(1, -1)   // Diagonal /
        )
        
        for ((deltaRow, deltaCol) in directions) {
            // Look for sequences of pieces and empty spaces that could lead to wins
            var playerCount = 0
            var emptyCount = 0
            var opponentCount = 0
            
            // Check 3 in each direction (4 total pieces including the current position)
            for (i in -3..3) {
                val r = row + i * deltaRow
                val c = col + i * deltaCol
                
                if (r in 0 until boardSize && c in 0 until boardSize) {
                    when (gameState.board[r][c]) {
                        playerValue -> playerCount++
                        EMPTY -> emptyCount++
                        opponent -> opponentCount++
                    }
                }
            }
            
            // Score based on piece configurations
            if (opponentCount == 0) {
                // No opponents in this line, so it's a potential win
                when (playerCount) {
                    3 -> score += 1000  // 3 in a row - very strong position
                    2 -> score += 100   // 2 in a row - good position
                    1 -> score += 10    // Just this piece - weak but potential
                }
            } else if (playerCount == 0 && opponentCount > 0) {
                // Defensive evaluation - block opponent's potential lines
                when (opponentCount) {
                    3 -> score -= 800   // Critical to block, but not as valuable as winning
                    2 -> score -= 50    // Should consider blocking
                }
            }
        }
        
        // Look for "trap" setups - situations where dropping a piece creates a fork
        val tempBoard = Array(boardSize) { r -> IntArray(boardSize) { c -> gameState.board[r][c] } }
        
        // Check if we can create a trap by playing here
        tempBoard[row][col] = playerValue
        
        // Check for potential next-move wins in multiple columns
        var winningColumns = 0
        for (nextCol in 0 until boardSize) {
            val nextRow = findBottomEmptyRow(gameState, nextCol)
            if (nextRow != -1) {
                tempBoard[nextRow][nextCol] = playerValue
                
                // Check if this would be a win
                var isWin = false
                for ((dr, dc) in directions) {
                    if (checkConnect4Line(tempBoard, nextRow, nextCol, dr, dc, playerValue)) {
                        isWin = true
                        break
                    }
                }
                
                if (isWin) {
                    winningColumns++
                }
                
                // Restore board
                tempBoard[nextRow][nextCol] = EMPTY
            }
        }
        
        // Significant bonus for moves that create multiple winning possibilities
        if (winningColumns >= 2) {
            score += 5000
        }
        
        return score
    }
    
    /**
     * Checks if there's a connect-4 win in a specific direction.
     */
    private fun checkConnect4Line(
        board: Array<IntArray>,
        row: Int, 
        col: Int, 
        deltaRow: Int, 
        deltaCol: Int, 
        playerValue: Int
    ): Boolean {
        val boardSize = board.size
        var count = 1  // Start with the current piece
        
        // Check in the positive direction
        for (i in 1..3) {
            val r = row + i * deltaRow
            val c = col + i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize && board[r][c] == playerValue) {
                count++
            } else {
                break
            }
        }
        
        // Check in the negative direction
        for (i in 1..3) {
            val r = row - i * deltaRow
            val c = col - i * deltaCol
            
            if (r in 0 until boardSize && c in 0 until boardSize && board[r][c] == playerValue) {
                count++
            } else {
                break
            }
        }
        
        return count >= 4
    }
}