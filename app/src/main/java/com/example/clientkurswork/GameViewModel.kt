package com.example.clientkurswork

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.clientkurswork.enums.CellType
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.BaseModel
import com.example.clientkurswork.models.CellModel
import com.example.clientkurswork.models.HomeModel
import com.example.clientkurswork.models.PieceModel
import com.example.clientkurswork.models.PlayerModel
import java.util.Queue

@SuppressLint("MutableCollectionMutableState")
class GameViewModel(playersQueue: Queue<PlayerModel>) : ViewModel() {
    private val engine = GameEngine(playersQueue)

    private var boardState by mutableStateOf(engine.board)
    fun getBoardStateSublist(from: Int, to: Int): MutableList<CellModel> {
        return boardState.subList(from - 1, to)
    }

    private var basesState by mutableStateOf(engine.bases)
    fun getBase(player: Player): BaseModel {
        return when (player) {
            Player.YELLOW -> basesState[0]
            Player.BLUE -> basesState[1]
            Player.RED -> basesState[2]
            Player.GREEN -> basesState[3]
        }
    }

    private var homesState by mutableStateOf(engine.homes)
    fun getHome(player: Player): HomeModel {
        return when (player) {
            Player.YELLOW -> homesState[0]
            Player.BLUE -> homesState[1]
            Player.RED -> homesState[2]
            Player.GREEN -> homesState[3]
        }
    }

    var movementPoints by mutableIntStateOf(engine.movementPoints)
        private set

    var currentPlayer: PlayerModel by mutableStateOf(engine.currentPlayer)
        private set

    val players = playersQueue

    private var movingCellId = -1
    fun setSelectedPiece(piece: PieceModel?) {
        if (engine.movementPoints != 0) {
            if (piece != null) {
                //Запоминаем фишку
                engine.selectedPiece = piece
                //Ищем куда ей можно походить
                movingCellId = engine.checkAvailableMoves(piece)
                if (movingCellId == -1) {
                    Log.d("Piece selecting", "Piece reset")
                    engine.selectedPiece = null
                    updateCellAvailability(movingCellId, false)
                    updateHomeAvailability(movingCellId, false)
                } else if (movingCellId > 400) {
                    Log.d("Piece selecting", "Home with id $movingCellId")
                    updateHomeAvailability(movingCellId, true)
                    updateCellAvailability(movingCellId, false)
                } else {
                    updateCellAvailability(movingCellId, true)
                    updateHomeAvailability(movingCellId, false)
                }
            } else {
                Log.d("Piece selecting", "Piece reset")
                engine.selectedPiece = null
                updateCellAvailability(movingCellId, false)
            }
            Log.d("Piece selecting", "$piece")
        }
    }

    private fun updateCellAvailability(cellId: Int, isAvailable: Boolean) {
        //Сперва зануляем всё
        boardState = boardState.map { cell ->
            cell.copy(availForMove = false)
        }.toMutableList()

        boardState = boardState.map { cell ->
            if (cell.ordinalNumber == cellId) cell.copy(availForMove = isAvailable)
            else cell
        }.toMutableList()
        engine.board = boardState
    }

    private fun updateHomeAvailability(homeId: Int, isAvailable: Boolean) {
        //Сперва зануляем всё
        homesState = homesState.map { home ->
            home.copy(availForMove = false)
        }.toMutableList()

        homesState = homesState.map { home ->
            if (home.id == homeId) home.copy(availForMove = isAvailable)
            else home
        }.toMutableList()
        engine.homes = homesState
    }

    var passMovingStatus by mutableStateOf(definePassMovingStatus())
        private set

    private fun definePassMovingStatus(): String {
        return if (engine.movementPoints == 6)
            ADDITIONAL_MOVE_MSG
        else
            PASS_MOVE_MSG
    }

    private var nMovesFromPlayer = 0
    private var lastMovedPiece: PieceModel? = null

    fun passMoving() {
        Log.d(
            "Pass moving", "Player - ${currentPlayer.onBoardMatching}," +
                    "status - $passMovingStatus"
        )
        updateCellAvailability(-1, false)
        updateHomeAvailability(-1, false)

        passMovingStatus = if (nMovesFromPlayer == 3) {
            if (lastMovedPiece != null) {
                val cellOfLastMovedPiece = engine.findPiece(lastMovedPiece!!.id)
                if (cellOfLastMovedPiece is CellModel) {
                    if (cellOfLastMovedPiece.type != CellType.HOMESTRETCH)
                        beatPiece(lastMovedPiece!!, cellOfLastMovedPiece, isPenaltyOfThird6 = true)
                    else
                        returnOnHomestretchBegin(lastMovedPiece!!, cellOfLastMovedPiece)
                }
            }
            engine.passMoving(isInterruptOfMoves = true)
        } else
            engine.passMoving(isInterruptOfMoves = false)
        if (passMovingStatus == ADDITIONAL_MOVE_MSG)
            nMovesFromPlayer++
        else {
            nMovesFromPlayer = 0
            lastMovedPiece = null
        }
        currentPlayer = engine.currentPlayer
        movementPoints = engine.movementPoints
    }

    fun makeMove() {
        //movingElement - кортеж из Boolean, Any? и Any?
        //В свою очередь, первый Any (ME.second) - CellModel, либо HomeModel
        // (либо null для невозможного хода).
        //Второй Any (ME.third) - BaseModel либо CellModel (либо null для невозможного хода)
        val movingElement = engine.move()
        if (!movingElement.first)
            return

        val movingPiece = engine.selectedPiece!!

        if (movingElement.second is CellModel) {
            val recipientCell = movingElement.second as CellModel
            if (movingElement.third is BaseModel) {
                //Обновляем базы
                basesState = basesState.map { base ->
                    if (base.player == movingPiece.player) {
                        // Создаем НОВЫЙ список фишек для этой базы без перемещаемой
                        val newPieces =
                            base.pieces.filter { it.id != movingPiece.id }.toMutableList()
                        base.copy(pieces = newPieces)
                    } else base
                }.toMutableList()

                //Обновляем ячейки
                boardState = boardState.map { cell ->
                    if (cell.ordinalNumber == recipientCell.ordinalNumber) {
                        // Создаем НОВЫЙ список фишек для ячейки + добавляем новую
                        val newPieces = (cell.pieces + movingPiece).toMutableList()
                        cell.copy(pieces = newPieces)
                    } else cell
                }.toMutableList()

                val victimPiece =
                    recipientCell.pieces.find { it.player != currentPlayer.onBoardMatching }
                if (victimPiece != null) {
                    beatPiece(victimPiece, recipientCell)
                }
            }

            if (movingElement.third is CellModel) {
                //Обновляем ячейки
                boardState = boardState.map { cell ->
                    if (cell.ordinalNumber == recipientCell.ordinalNumber) {
                        // Создаем НОВЫЙ список фишек для ячейки + добавляем новую
                        val newPieces = (cell.pieces + movingPiece).toMutableList()
                        cell.copy(pieces = newPieces)
                    } //Убираем фишку из исходной ячейки
                    else if (cell.ordinalNumber == (movingElement.third as CellModel).ordinalNumber) {
                        val newPieces = (cell.pieces - movingPiece).toMutableList()
                        cell.copy(pieces = newPieces)
                    } else
                        cell
                }.toMutableList()

                if (recipientCell.type != CellType.SAFE && recipientCell.type != CellType.START) {
                    //Если нашли чужие фишки в точке перехода - съедаем чужака
                    val victimPiece =
                        recipientCell.pieces.find { it.player != currentPlayer.onBoardMatching }
                    if (victimPiece != null) {
                        beatPiece(victimPiece, recipientCell)
                    }
                }
            }
            updateCellAvailability(recipientCell.ordinalNumber, false)

        } else if (movingElement.second is HomeModel) {
            val recipientHome = movingElement.second as HomeModel
            Log.d("Home moving", "Home - ${recipientHome.id}")

            //Обновляем ячейки
            boardState = boardState.map { cell ->
                if (cell.ordinalNumber == (movingElement.third as CellModel).ordinalNumber) {
                    val newPieces = (cell.pieces - movingPiece).toMutableList()
                    cell.copy(pieces = newPieces)
                } else
                    cell
            }.toMutableList()

            homesState = homesState.map { home ->
                if (home.player == recipientHome.player) {
                    val newPieces = (home.pieces + movingPiece).toMutableList()
                    home.copy(pieces = newPieces)
                } else
                    home
            }.toMutableList()

            updateHomeAvailability(recipientHome.id, false)

            //Начисляем игроку 10 очков по правилам парчиса
            movementPoints = 10
            engine.movementPoints = 10
        }

        // 3. Сброс фишки
        lastMovedPiece = engine.selectedPiece
        engine.selectedPiece = null

        // 4. Обновляем списки в движке для того, чтобы функции поиска
        //допустимых ходов работали нормально
        engine.board = boardState
        engine.bases = basesState
        engine.homes = homesState

        if (movementPoints != 10)
            movementPoints = engine.movementPoints
    }

    private fun beatPiece(
        victimPiece: PieceModel,
        recipientCell: CellModel,
        isPenaltyOfThird6: Boolean = false
    ) {
        //Обновляем базы
        basesState = basesState.map { base ->
            if (base.player == victimPiece.player) {
                // Создаем НОВЫЙ список фишек для этой базы без перемещаемой
                val newPieces = (base.pieces + victimPiece).toMutableList()
                base.copy(pieces = newPieces)
            } else base
        }.toMutableList()

        //Обновляем ячейки
        boardState = boardState.map { cell ->
            if (cell.ordinalNumber == recipientCell.ordinalNumber) {
                // Создаем НОВЫЙ список фишек для ячейки + добавляем новую
                val newPieces = (cell.pieces - victimPiece).toMutableList()
                cell.copy(pieces = newPieces)
            } else cell
        }.toMutableList()

        if (!isPenaltyOfThird6) {
            //Очки движения за "съедание" фишки
            engine.movementPoints = 20
            movementPoints = 20
        }
    }

    private fun returnOnHomestretchBegin(victimPiece: PieceModel, recipientCell: CellModel) {
        //Обновляем ячейки
        boardState = boardState.map { cell ->
            if (cell.ordinalNumber == recipientCell.ordinalNumber) {
                // Создаем НОВЫЙ список фишек для ячейки + добавляем новую
                val newPieces = (cell.pieces - victimPiece).toMutableList()
                cell.copy(pieces = newPieces)
            } else if (cell.ordinalNumber == FIRST_HOMESTRETCH_CELLS[victimPiece.player]) {
                val newPieces = (cell.pieces + victimPiece).toMutableList()
                cell.copy(pieces = newPieces)
            } else cell
        }.toMutableList()
    }
}