#include "Game.h"
#include "Room.h"
#include "Server.h"

#include <random>

/*Game::Game(Server &serv, Room &room_) : server(serv), room(room_) {
    engine.seed(std::time(nullptr));
}*/

Game::Game(Room& room_) : room(room_) {
    engine.seed(std::time(nullptr));
}

void Game::startGame() {
    auto nPlayers = 0;
    for (auto &place: room.getPlayers()) {
        if (!place.username.empty())
            nPlayers++;
    }
    room.setPlayers(nPlayers);

    rollDice();
}

void Game::rollDice() {
    currentPlayerNumber++;

    std::uniform_int_distribution<char> dice(1, 6);
    auto result = dice(engine);

    auto players = room.getPlayers();

    std::stringstream ss;
    ss << ROLLED << " " << Server::u8StringToString(players[currentPlayerNumber % players.size()].username)
       << " " << result;

    //server.allPlayersSending(&room, ss.str(), u8"", false);
    std::cout << ss.str() << std::endl;
}