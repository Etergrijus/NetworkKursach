package com.example.clientkurswork

import android.util.Log
import com.example.clientkurswork.models.BaseModel
import com.example.clientkurswork.models.CellModel
import com.example.clientkurswork.enums.CellType
import com.example.clientkurswork.models.HomeModel
import com.example.clientkurswork.models.PieceModel
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.PlayerModel
import java.util.Queue

private const val BOARD_SIZE = 96
private const val FIRST_HOMESTRETCH_CELL = 69
private const val HOMESTRETCH_CELLS = 7
private val START_CELLS = mapOf(
    4 to Player.YELLOW,
    21 to Player.BLUE,
    38 to Player.RED,
    55 to Player.GREEN
)
private val TURN_ON_HOMESTRETCH_CELLS = mapOf(
    17 to Player.BLUE,
    34 to Player.RED,
    51 to Player.GREEN,
    68 to Player.YELLOW
)
val FIRST_HOMESTRETCH_CELLS = mapOf(
    Player.YELLOW to 69,
    Player.BLUE to 76,
    Player.RED to 83,
    Player.GREEN to 90
)
private val FINISH_VALUES = mapOf(
    76 to Player.YELLOW,
    83 to Player.BLUE,
    90 to Player.RED,
    97 to Player.GREEN
)
private val SAFE_CELLS = intArrayOf(13, 17, 30, 34, 47, 51, 64, 68)

const val ADDITIONAL_MOVE_MSG = "Бросить кубик"
const val PASS_MOVE_MSG = "Передать ход"

class GameEngine(playersQueue: Queue<PlayerModel>) {
    var board: MutableList<CellModel> = mutableListOf()

    //private set
    var bases: MutableList<BaseModel> = mutableListOf()

    //private set
    var homes: MutableList<HomeModel> = mutableListOf()
    //private set

    init {
        //Инициализуем игровое поле(доску)
        for (i in 1..BOARD_SIZE) {
            when {
                START_CELLS.containsKey(i) -> {
                    val player = START_CELLS[i]!!
                    board.add(CellModel(i, CellType.START, mutableListOf(), player))
                }

                i in SAFE_CELLS -> board.add(CellModel(i, CellType.SAFE, mutableListOf()))

                i in FIRST_HOMESTRETCH_CELL..BOARD_SIZE -> {
                    //Определяем какому игроку принадлежит финишная прямая
                    val player = when ((i - FIRST_HOMESTRETCH_CELL) / HOMESTRETCH_CELLS) {
                        0 -> Player.YELLOW
                        1 -> Player.BLUE
                        2 -> Player.RED
                        3 -> Player.GREEN
                        else -> throw IllegalStateException("unknown value of player in board creation")
                    }

                    board.add(
                        CellModel(
                            i,
                            CellType.HOMESTRETCH,
                            mutableListOf(),
                            player
                        )
                    )
                }

                else -> board.add(CellModel(i, CellType.PATH, mutableListOf()))
            }
        }

        for (i in 1..4) {
            val player: Player = when (i) {
                1 -> Player.YELLOW
                2 -> Player.BLUE
                3 -> Player.RED
                4 -> Player.GREEN
                else -> throw IllegalStateException("unknown value of player in bases creation")
            }

            bases.add(
                BaseModel(player,
                    MutableList(4) { pieceIndex ->
                        PieceModel(
                            pieceIndex + 1 + 4 * (i - 1),
                            player
                        )
                    })
            )
            homes.add(HomeModel(400 + i, player, mutableListOf()))
        }
    }

    private var diceRollResult = diceRoll()
    var movementPoints = diceRollResult

    private fun diceRoll(): Int {
        return (1..6).random()
    }

    private val players = playersQueue
    var currentPlayer: PlayerModel = players.peek()!!
        private set

    var selectedPiece: PieceModel? = null

    fun checkAvailableMoves(piece: PieceModel): Int {
        val pieceMasterElement: Any = findPiece(piece.id) ?: return -1

        if (pieceMasterElement is BaseModel) {
            Log.d("Piece selecting", "Ready for move from base for ${piece.id}")
            if (movementPoints == 5) {
                val startCell =
                    board.find {
                        it.type == CellType.START && it.player == pieceMasterElement.player
                    }!!
                if (startCell.pieces.size == 2
                    && startCell.pieces.find { it.player != pieceMasterElement.player } != null
                )
                    return -1

                Log.d("Piece selecting", "Cell is ${startCell.ordinalNumber}")
                return startCell.ordinalNumber
            } else
                return -1
        } else if (pieceMasterElement is CellModel) {
            Log.d(
                "Piece selecting",
                "Ready for move from cell ${pieceMasterElement.ordinalNumber} for ${piece.id}"
            )

            var recipientCell: CellModel = pieceMasterElement

            if (movementPoints in 6..7) {
                val cellsWherePiecesInBlocks =
                    board.filter { index ->
                        index.pieces.size == 2 &&
                                index.pieces.find {
                                    it.player == currentPlayer.onBoardMatching
                                } != null
                    }
                if (cellsWherePiecesInBlocks.isNotEmpty() &&
                    recipientCell !in cellsWherePiecesInBlocks)
                    return -1
            }

            //Если фишка стоит на повороте, надо повернуть её сразу
            if (TURN_ON_HOMESTRETCH_CELLS.containsKey(recipientCell.ordinalNumber) &&
                currentPlayer.onBoardMatching
                == TURN_ON_HOMESTRETCH_CELLS[recipientCell.ordinalNumber]
            ) {
                Log.d(
                    "Piece moving", "Player - ${currentPlayer.onBoardMatching}," +
                            "cell - ${recipientCell.ordinalNumber}"
                )
                return turnOnHomestretch(
                    movementPoints,
                    currentPlayer.onBoardMatching,
                    recipientCell.ordinalNumber
                )
            }

            //Проверка барьеров
            for (i in 1..movementPoints) {
                //Все игроки кроме жёлтого будут вынуждены проходить круг, переходя из 68-ой ячейки
                //в первую. С этим надо быть осторожным
                var index = (pieceMasterElement.ordinalNumber - 1) + i
                //Индекс корректируем на 1, так как индексы от 0. Поэтому корректировка на 68,
                //а не на 69
                //Разумеется, если фишка перешла на финишную прямую, к ней корректировка не применяется
                if (index >= 68 && pieceMasterElement.type != CellType.HOMESTRETCH
                    && currentPlayer.onBoardMatching != Player.YELLOW
                )
                    index -= 68

                if (index == 96 && currentPlayer.onBoardMatching == Player.GREEN) {
                    //Если ровно вписались в финиш - дошли. Иначе ход невозможен
                    return if (i == movementPoints)
                        homes.find { it.player == currentPlayer.onBoardMatching }!!.id
                    else
                        -1
                }

                //Нашли барьер? Ход невозможен
                if (board[index].pieces.size == 2) {
                    return -1
                }

                recipientCell = board[index]

                //Если фишка достигает поворота на финишную прямую - передаём управление
                //её оставшимся движением функции turnOnHomestretch
                if (TURN_ON_HOMESTRETCH_CELLS.containsKey(recipientCell.ordinalNumber)
                    && currentPlayer.onBoardMatching
                    == TURN_ON_HOMESTRETCH_CELLS[recipientCell.ordinalNumber]
                ) {
                    Log.d(
                        "Piece moving", "Player - ${currentPlayer.onBoardMatching}," +
                                "cell - ${recipientCell.ordinalNumber}"
                    )
                    return turnOnHomestretch(
                        movementPoints - i,
                        currentPlayer.onBoardMatching,
                        recipientCell.ordinalNumber
                    )
                }

                //Проверяем дошли ли до финиша
                if (FINISH_VALUES.containsKey(recipientCell.ordinalNumber) &&
                    currentPlayer.onBoardMatching == FINISH_VALUES[recipientCell.ordinalNumber]
                ) {
                    //Если ровно вписались в финиш - дошли. Иначе ход невозможен
                    return if (i == movementPoints)
                        homes.find { it.player == currentPlayer.onBoardMatching }!!.id
                    else
                        -1
                }
            }
            return recipientCell.ordinalNumber
        }
        return -1
    }

    fun findPiece(pieceId: Int): Any? {
        for (i in 0..<board.size) {
            if (board[i].pieces.find { it.id == pieceId } != null) {
                return board[i]
            }
        }

        for (i in 0..<bases.size) {
            if (bases[i].pieces.find { it.id == pieceId } != null) {
                return bases[i]
            }
        }

        return null
    }

    private fun turnOnHomestretch(
        remainMovementPoints: Int,
        player: Player,
        startCellId: Int
    ): Int {
        var recipientCellId = startCellId
        for (i in 1..remainMovementPoints) {
            recipientCellId = FIRST_HOMESTRETCH_CELLS[player]?.plus(i - 1) ?: return -1

            //Проверяем дошли ли до финиша
            if (FINISH_VALUES.containsKey(recipientCellId) &&
                currentPlayer.onBoardMatching == FINISH_VALUES[recipientCellId]
            ) {
                //Если ровно вписались в финиш - дошли. Иначе ход невозможен
                return if (i == movementPoints)
                    homes.find { it.player == currentPlayer.onBoardMatching }!!.id
                else
                    -1
            }
        }
        return recipientCellId
    }

    private var nMoves = 1

    fun passMoving(isInterruptOfMoves: Boolean = false): String {
        selectedPiece = null

        val oldDiceRollResult = diceRollResult
        Log.d("Pass moving", "Old Dice - $oldDiceRollResult")
        if (oldDiceRollResult == 6 && !isInterruptOfMoves) {
            diceRollResult = diceRoll()
            movementPoints = diceRollResult
            //По правилам, если все фишки игрока выведены с базы, его 6 заменяется на 7
            if (bases.find { it.player == currentPlayer.onBoardMatching }?.pieces?.size == 0)
                movementPoints = 7
            return if (diceRollResult == 6)
                ADDITIONAL_MOVE_MSG
            else
                PASS_MOVE_MSG
        } else {
            val movedPlayer = players.poll()
            players.add(movedPlayer)
            currentPlayer = players.peek()!!
        }

        diceRollResult = diceRoll()
        movementPoints = diceRollResult
        if (diceRollResult == 6) {
            //По правилам, если все фишки игрока выведены с базы, его 6 заменяется на 7
            if (bases.find { it.player == currentPlayer.onBoardMatching }?.pieces?.size == 0)
                movementPoints = 7
            return ADDITIONAL_MOVE_MSG
        }
        return PASS_MOVE_MSG
    }

    fun move(): Triple<Boolean, Any?, Any?> {
        val piece = selectedPiece ?: return Triple(false, null, null)
        val master = findPiece(piece.id) ?: return Triple(false, null, null)

        // Просто проверяем возможность
        val recipientCell = board.find { it.availForMove }
        if (recipientCell == null) {
            val recipientHome = homes.find { it.availForMove }
            if (recipientHome != null)
                Log.d("Home moving", "Found home ${recipientHome.id}")
            return if (recipientHome == null)
                Triple(false, null, null)
            else
                Triple(true, recipientHome, master)
        }

        movementPoints = 0
        // НИКАКОГО .add() или .remove() здесь!
        return Triple(true, recipientCell, master)
    }
}