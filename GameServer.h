#ifndef SERVERKURSSWORK_GAMESERVER_H
#define SERVERKURSSWORK_GAMESERVER_H

#include "Room.h"

#include "Game.h"

class GameServer {
public:
    explicit GameServer(Room& _room);

    void startRead(const std::shared_ptr<tcpAlias::socket> &socket);

    void startWrite();

private:
    void handleRead();

    void handleWrite();

    Room room;
    Game* gameInstance;
};


#endif //SERVERKURSSWORK_GAMESERVER_H
