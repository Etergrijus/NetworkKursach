package com.example.clientkurswork.views

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.clientkurswork.GameViewModel
import com.example.clientkurswork.enums.CellTextDirection
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.BaseModel
import com.example.clientkurswork.models.CellModel
import com.example.clientkurswork.models.HomeModel
import com.example.clientkurswork.models.PieceModel
import com.example.clientkurswork.models.PlayerModel

@SuppressLint("ConfigurationScreenWidthHeight", "UseOfNonLambdaOffsetOverload")
@Composable
fun Main(viewModel: GameViewModel) {
    //Блок для создания полного экрана без системных элементов
    val view = LocalView.current
    val window = (view.context as Activity).window
    val controller = WindowCompat.getInsetsController(window, view)
    SideEffect {
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }


    //Переменные для расчёта размеров финишных блоков на игровом поле
    var finishBlockWidth by remember { mutableFloatStateOf(0f) }
    var finishBlockHeight by remember { mutableFloatStateOf(0f) }

    //Блок полного экрана
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xff966F33))
            .clickable(
                //Выключаем визуальный эффект нажатия
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.setSelectedPiece(null)
            }
    ) {
        //Блок рабочей зоны
        ConstraintLayout(
            Modifier
                .fillMaxSize()
                .padding(
                    start = WindowInsets.displayCutout.asPaddingValues()
                        .calculateStartPadding(LayoutDirection.Ltr)
                )
        ) {
            val (text1, text2, text3, text4, diceRollBox, passMoveBtn, gameField) = createRefs()
            val verticalGuideline = createGuidelineFromStart(fraction = .20f)
            val horizontalGuideline = createGuidelineFromTop(fraction = .45f)

            Box(Modifier.constrainAs(text1) {
                top.linkTo(parent.top, margin = 30.dp)
                end.linkTo(verticalGuideline, margin = 20.dp)
                bottom.linkTo(diceRollBox.top, margin = 30.dp)
                start.linkTo(parent.start, margin = 20.dp)
                width = Dimension.fillToConstraints
            }, contentAlignment = Alignment.Center) {
                Nickname(viewModel.players.filter { it.onBoardMatching == Player.BLUE }[0].username)
            }

            //Вывод результата кубика
            Column(Modifier.constrainAs(diceRollBox) {
                top.linkTo(horizontalGuideline)
                end.linkTo(verticalGuideline, margin = 20.dp)
                start.linkTo(parent.start, margin = 20.dp)
                width = Dimension.fillToConstraints
            }, horizontalAlignment = Alignment.CenterHorizontally) {
                val diceRoll = viewModel.movementPoints
                val currentPlayer = viewModel.currentPlayer
                Text(
                    text = "Ход ${currentPlayer.username}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Очков движения:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(text = diceRoll.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Box(Modifier.constrainAs(text2) {
                bottom.linkTo(parent.bottom, margin = 30.dp)
                end.linkTo(verticalGuideline, margin = 20.dp)
                top.linkTo(diceRollBox.bottom, margin = 30.dp)
                start.linkTo(parent.start, margin = 20.dp)
                width = Dimension.fillToConstraints
            }, contentAlignment = Alignment.Center) {
                Nickname(viewModel.players.filter { it.onBoardMatching == Player.RED }[0].username)
            }

            //Игровое поле
            Column(Modifier
                .width(Dp((LocalConfiguration.current.screenHeightDp * 1.25).toFloat()))
                .border(width = 2.dp, color = Color.Black)
                .constrainAs(gameField) {
                    height = Dimension.fillToConstraints
                    top.linkTo(parent.top, margin = 20.dp)
                    bottom.linkTo(parent.bottom, margin = 20.dp)
                    start.linkTo(verticalGuideline)
                }
                .background(color = Color.White)) {
                //Первая строка
                Row(
                    Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = Color.Black)
                        .weight(1f)
                ) {
                    BaseBlock(
                        getBase = viewModel::getBase,
                        selectPiece = viewModel::setSelectedPiece,
                        currentPlayer = viewModel.currentPlayer,
                        Player.BLUE,
                        Modifier.weight(1f)
                    )

                    //Лепесток с синей финишной прямой
                    Row(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(width = 1.dp, color = Color.Black)
                    ) {
                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 18,
                            endCellIndex = 25,
                            Modifier.weight(1f),
                            isHorizontal = false,
                            cellTextDirection = CellTextDirection.STRAIGHT
                        )

                        //Средняя линия - помимо финишной прямой нужно
                        //ещё сделать единственную ячейку обычного пути
                        Column(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .border(width = 1.dp, color = Color.Black)
                        ) {
                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 17,
                                endCellIndex = 17,
                                Modifier.weight(1f),
                                isHorizontal = false,
                                cellTextDirection = CellTextDirection.STRAIGHT
                            )

                            //Синяя финишная прямая
                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 76,
                                endCellIndex = 82,
                                Modifier.weight(7f),
                                isHorizontal = false,
                            )
                        }


                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 9,
                            endCellIndex = 16,
                            Modifier.weight(1f),
                            isReversedLine = true,
                            isHorizontal = false,
                            cellTextDirection = CellTextDirection.STRAIGHT
                        )
                    }

                    BaseBlock(
                        getBase = viewModel::getBase,
                        selectPiece = viewModel::setSelectedPiece,
                        currentPlayer = viewModel.currentPlayer,
                        Player.YELLOW,
                        Modifier.weight(1f)
                    )
                }

                //Вторая строка
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(width = 1.dp, color = Color.Black)
                ) {
                    //Лепесток с красной финишной прямой
                    Column(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(width = 1.dp, color = Color.Black)
                    ) {
                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 26,
                            endCellIndex = 33,
                            Modifier.weight(1f),
                            isReversedLine = true,
                            cellTextDirection = CellTextDirection.ON_RIGHT_SIDE,
                        )

                        //Средняя линия - помимо финишной прямой нужно
                        //ещё сделать единственную ячейку обычного пути
                        Row(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .border(width = 1.dp, color = Color.Black)
                        ) {
                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 34,
                                endCellIndex = 34,
                                Modifier.weight(1f),
                                cellTextDirection = CellTextDirection.ON_RIGHT_SIDE,
                            )

                            //Красная финишная прямая
                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 83,
                                endCellIndex = 89,
                                Modifier.weight(7f),
                            )
                        }


                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 35,
                            endCellIndex = 42,
                            Modifier.weight(1f),
                            cellTextDirection = CellTextDirection.ON_RIGHT_SIDE,
                        )
                    }

                    //Блок домов игроков
                    Box(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(width = 1.dp, color = Color.Black)
                            //Рассчёт координат
                            .onGloballyPositioned { coordinates ->
                                finishBlockWidth = coordinates.size.width.toFloat()
                                finishBlockHeight = coordinates.size.height.toFloat()
                            }) {
                        HomeBlock(
                            viewModel.getHome(Player.BLUE),
                            Pair(0f, 0f),
                            Pair(finishBlockWidth, 0f),
                            finishBlockWidth,
                            finishBlockHeight,
                            viewModel::setSelectedPiece,
                            viewModel::makeMove,
                            isHorizontal = true,
                            isInTopLeft = true
                        )

                        HomeBlock(
                            viewModel.getHome(Player.RED),
                            Pair(0f, 0f),
                            Pair(0f, finishBlockHeight),
                            finishBlockWidth,
                            finishBlockHeight,
                            viewModel::setSelectedPiece,
                            viewModel::makeMove,
                            isHorizontal = false,
                            isInTopLeft = true
                        )

                        HomeBlock(
                            viewModel.getHome(Player.GREEN),
                            Pair(0f, finishBlockHeight),
                            Pair(finishBlockWidth, finishBlockHeight),
                            finishBlockWidth,
                            finishBlockHeight,
                            viewModel::setSelectedPiece,
                            viewModel::makeMove,
                            isHorizontal = true,
                            isInTopLeft = false
                        )

                        HomeBlock(
                            viewModel.getHome(Player.YELLOW),
                            Pair(finishBlockWidth, 0f),
                            Pair(finishBlockWidth, finishBlockHeight),
                            finishBlockWidth,
                            finishBlockHeight,
                            viewModel::setSelectedPiece,
                            viewModel::makeMove,
                            isHorizontal = false,
                            isInTopLeft = false
                        )

                        //Рисуем "границы" между домами
                        Canvas(Modifier.fillMaxSize()) {
                            drawLine(
                                color = Color.Black,
                                start = Offset(x = 0f, y = 0f),
                                end = Offset(x = finishBlockWidth, y = finishBlockHeight),
                                strokeWidth = 6f
                            )

                            drawLine(
                                color = Color.Black,
                                start = Offset(x = finishBlockWidth, y = 0f),
                                end = Offset(x = 0f, y = finishBlockHeight),
                                strokeWidth = 6f
                            )
                        }
                    }

                    //Лепесток с жёлтой финишной прямой
                    Column(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(width = 1.dp, color = Color.Black)
                    ) {
                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 1,
                            endCellIndex = 8,
                            Modifier.weight(1f),
                            isReversedLine = true,
                            cellTextDirection = CellTextDirection.ON_LEFT_SIDE,
                        )

                        //Средняя линия - помимо финишной прямой нужно
                        //ещё сделать единственную ячейку обычного пути
                        Row(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .border(width = 1.dp, color = Color.Black)
                        ) {
                            //Жёлтая финишная прямая
                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 69,
                                endCellIndex = 75,
                                Modifier.weight(7f),
                                isReversedLine = true,
                            )

                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 68,
                                endCellIndex = 68,
                                Modifier.weight(1f),
                                cellTextDirection = CellTextDirection.ON_LEFT_SIDE,
                            )
                        }


                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 60,
                            endCellIndex = 67,
                            Modifier.weight(1f),
                            cellTextDirection = CellTextDirection.ON_LEFT_SIDE,
                        )
                    }
                }

                //Третья строка
                Row(
                    Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = Color.Black)
                        .weight(1f)
                ) {
                    BaseBlock(
                        getBase = viewModel::getBase,
                        selectPiece = viewModel::setSelectedPiece,
                        currentPlayer = viewModel.currentPlayer,
                        Player.RED,
                        Modifier.weight(1f)
                    )

                    //Лепесток с зелёной финишной прямой
                    Row(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .border(width = 1.dp, color = Color.Black)
                    ) {
                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 43,
                            endCellIndex = 50,
                            Modifier.weight(1f),
                            isHorizontal = false,
                            cellTextDirection = CellTextDirection.STRAIGHT,
                        )

                        //Средняя линия - помимо финишной прямой нужно
                        //ещё сделать единственную ячейку обычного пути
                        Column(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .border(width = 1.dp, color = Color.Black)
                        ) {
                            //Зелёная финишная прямая
                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 90,
                                endCellIndex = 96,
                                Modifier.weight(7f),
                                isReversedLine = true,
                                isHorizontal = false,
                            )

                            CellLine(
                                getSublist = viewModel::getBoardStateSublist,
                                selectPiece = viewModel::setSelectedPiece,
                                currentPlayer = viewModel.currentPlayer,
                                makeMove = viewModel::makeMove,
                                startCellIndex = 51,
                                endCellIndex = 51,
                                Modifier.weight(1f),
                                isHorizontal = false,
                                cellTextDirection = CellTextDirection.STRAIGHT,
                            )
                        }


                        CellLine(
                            getSublist = viewModel::getBoardStateSublist,
                            selectPiece = viewModel::setSelectedPiece,
                            currentPlayer = viewModel.currentPlayer,
                            makeMove = viewModel::makeMove,
                            startCellIndex = 52,
                            endCellIndex = 59,
                            Modifier.weight(1f),
                            isReversedLine = true,
                            isHorizontal = false,
                            cellTextDirection = CellTextDirection.STRAIGHT,
                        )
                    }

                    BaseBlock(
                        getBase = viewModel::getBase,
                        selectPiece = viewModel::setSelectedPiece,
                        currentPlayer = viewModel.currentPlayer,
                        player = Player.GREEN,
                        Modifier.weight(1f)
                    )
                }
            }

            Box(Modifier.constrainAs(text3) {
                top.linkTo(parent.top, margin = 30.dp)
                start.linkTo(gameField.end, margin = 20.dp)
                bottom.linkTo(passMoveBtn.top, margin = 30.dp)
                end.linkTo(parent.end, margin = 20.dp)
                width = Dimension.fillToConstraints
            }, contentAlignment = Alignment.Center) {
                Nickname(viewModel.players.filter { it.onBoardMatching == Player.YELLOW }[0].username)
            }

            Box(Modifier.constrainAs(passMoveBtn) {
                top.linkTo(horizontalGuideline)
                start.linkTo(gameField.end, margin = 50.dp)
                end.linkTo(parent.end, margin = 50.dp)
            }) {
                Button(
                    onClick = { viewModel.passMoving() },
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Gray,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        viewModel.passMovingStatus,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(Modifier.constrainAs(text4) {
                bottom.linkTo(parent.bottom, margin = 30.dp)
                start.linkTo(gameField.end, margin = 20.dp)
                top.linkTo(passMoveBtn.bottom, margin = 30.dp)
                end.linkTo(parent.end, margin = 20.dp)
                width = Dimension.fillToConstraints
            }, contentAlignment = Alignment.Center) {
                Nickname(viewModel.players.filter { it.onBoardMatching == Player.GREEN }[0].username)
            }
        }
    }
}

@Composable
private fun CellLine(
    getSublist: (Int, Int) -> List<CellModel>,
    selectPiece: (PieceModel?) -> Unit,
    currentPlayer: PlayerModel,
    makeMove: () -> Unit,
    startCellIndex: Int,
    endCellIndex: Int,
    modifier: Modifier = Modifier,
    isReversedLine: Boolean = false,
    isHorizontal: Boolean = true,
    cellTextDirection: CellTextDirection? = null,
) {
    val containerModifier = Modifier
        .fillMaxSize()
        .border(width = 1.dp, color = Color.Black)
        .then(modifier)

    val cells = if (isReversedLine)
        getSublist(startCellIndex, endCellIndex).reversed()
    else
        getSublist(startCellIndex, endCellIndex)

    if (isHorizontal)
        Row(containerModifier) {
            CellsContent(cells, cellTextDirection, selectPiece, makeMove, currentPlayer)
        }
    else
        Column(containerModifier) {
            // Используем RowScope.() -> Unit, но приводим к ColumnScope
            // чтобы .weight() работал
            CellsContent(cells, cellTextDirection, selectPiece, makeMove, currentPlayer)
        }
}

// Вспомогательная функция, которая принимает нужный Scope
@Composable
private fun RowScope.CellsContent(
    cells: List<CellModel>,
    cellTextDirection: CellTextDirection? = null,
    selectPiece: (PieceModel?) -> Unit,
    makeMove: () -> Unit,
    currentPlayer: PlayerModel,
) {
    cells.forEach { cell ->
        Box(
            Modifier
                .weight(1f) // weight доступен в RowScope
                .fillMaxSize()
                .border(1.dp, Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CellView(cell, cellTextDirection, makeMove)
            Column(Modifier.fillMaxHeight()) {
                cell.pieces.forEach {
                    PieceView(
                        it,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(5.dp),
                        onClick = selectPiece,
                        currentPlayer = currentPlayer
                    )
                }
            }
            if (cell.availForMove) {
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.size(30.dp)) {
                        val borderWidth = 2.dp.toPx()
                        val radius = 30f - borderWidth
                        drawCircle(
                            color = Color(0x99808080), radius = radius
                        )
                        //Контур
                        drawCircle(
                            color = Color.Black,
                            radius = radius,
                            style = Stroke(width = borderWidth)
                        )
                    }
                }
            }
        }
    }
}

// Для Column, нам нужна аналогичная функция
@Composable
private fun ColumnScope.CellsContent(
    cells: List<CellModel>,
    cellTextDirection: CellTextDirection? = null,
    selectPiece: (PieceModel?) -> Unit,
    makeMove: () -> Unit,
    currentPlayer: PlayerModel,
) {
    cells.forEach { cell ->
        Box(
            Modifier
                .weight(1f) // weight доступен в ColumnScope
                .fillMaxSize()
                .border(1.dp, Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CellView(cell, cellTextDirection, makeMove)
            Row(Modifier.fillMaxHeight()) {
                cell.pieces.forEach {
                    PieceView(
                        it,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(5.dp),
                        onClick = selectPiece,
                        currentPlayer = currentPlayer
                    )
                }
            }
            //Подсветка возможного хода
            if (cell.availForMove) {
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.size(30.dp)) {
                        val borderWidth = 2.dp.toPx()
                        val radius = 30f - borderWidth
                        drawCircle(
                            color = Color(0x99808080), radius = radius
                        )
                        //Контур
                        drawCircle(
                            color = Color.Black,
                            radius = radius,
                            style = Stroke(width = borderWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseBlock(
    getBase: (Player) -> BaseModel,
    selectPiece: (PieceModel?) -> Unit,
    currentPlayer: PlayerModel,
    player: Player,
    modifier: Modifier = Modifier
) {
    Box(
        Modifier
            .fillMaxSize()
            .border(width = 1.dp, color = Color.Black)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        val base = getBase(player)
        BaseView(base)
        FlowRow(
            Modifier.fillMaxSize(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center
        ) {
            base.pieces.forEach { piece ->
                PieceView(
                    piece,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(5.dp),
                    onClick = selectPiece,
                    currentPlayer = currentPlayer
                )
            }
        }
    }
}

@Composable
private fun HomeBlock(
    home: HomeModel,
    point1: Pair<Float, Float>,
    point2: Pair<Float, Float>,
    parentWidth: Float,
    parentHeight: Float,
    selectPiece: (PieceModel?) -> Unit,
    makeMove: () -> Unit,
    isHorizontal: Boolean,
    isInTopLeft: Boolean,
) {
    Box(
        Modifier.fillMaxSize(),
    ) {
        HomeView(
            point1,
            point2,
            home,
        )

        val width: Dp
        val height: Dp
        val offsetX: Dp
        val offsetY: Dp
        if (isHorizontal) {
            width = (parentWidth / 4).dp
            height = (parentHeight / 6).dp
            offsetX = 20.dp
            offsetY = if (isInTopLeft)
                1.dp
            else
                height
        } else {
            width = (parentWidth / 6).dp
            height = (parentHeight / 4).dp
            offsetX = if (isInTopLeft)
                1.dp
            else
                width
            offsetY = 20.dp
        }
        FlowRow(
            Modifier
                .width(width)
                .height(height)
                .offset(
                    x = offsetX,
                    y = offsetY
                ),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Top
        ) {
            home.pieces.forEach { piece ->
                PieceView(
                    piece,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(5.dp),
                    onClick = selectPiece,
                    currentPlayer = null
                )
            }
        }
        if (home.availForMove) {
            Box(
                Modifier
                    .width(width)
                    .height(height)
                    .offset(
                        x = offsetX,
                        y = offsetY
                    )
                    .clickable(
                        //Выключаем визуальный эффект нажатия
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        makeMove()
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(30.dp)) {
                    val borderWidth = 2.dp.toPx()
                    val radius = 30f - borderWidth
                    drawCircle(
                        color = Color(0x99808080), radius = radius
                    )
                    //Контур
                    drawCircle(
                        color = Color.Black,
                        radius = radius,
                        style = Stroke(width = borderWidth)
                    )
                }
            }
        }
    }
}


@Composable
private fun Nickname(nick: String) {
    Text(
        nick,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}