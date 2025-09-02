#include "GameServer.h"

GameServer::GameServer(Room &_room) : room(_room) {
    gameInstance = new Game(room);

    for (auto &player: room.getPlayers())
        startRead(player.socket);

    gameInstance->startGame();
}

void GameServer::startRead(const std::shared_ptr<tcpAlias::socket> &socket) {

}


