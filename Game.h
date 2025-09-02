#ifndef SERVERKURSSWORK_GAME_H
#define SERVERKURSSWORK_GAME_H

#include <random>

#include "Room.h"

#define ROLLED "Rolled"

//class Server;

class Game {
public:
    //Game(Server& serv, Room& room_);

    explicit Game(Room& room_);

    void startGame();

    void rollDice();

    void foo();

private:
    //Server& server;
    Room& room;
    std::mt19937 engine;
    long long currentPlayerNumber = -1;
};


#endif //SERVERKURSSWORK_GAME_H
