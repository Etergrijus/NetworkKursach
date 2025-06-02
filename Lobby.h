#ifndef SERVERKURSSWORK_LOBBY_H
#define SERVERKURSSWORK_LOBBY_H

#include <vector>
#include <string>
#include <unordered_map>
#include <random>

#include "Room.h"

class Lobby {
public:
    Lobby() : engine(std::random_device{}()) {
        lobby.reserve(5);
    }

    void findRoom(const std::u8string& username) {
        if (lobby.empty())
            addRoom();
        else {
            std::uniform_int_distribution<size_t> dist(0, lobby.size() - 1);
            for (size_t nTries = 0; nTries <= 10; nTries++) {
                size_t roomIndex = dist(engine);
                auto room = lobby[roomIndex];
                if (!room.isRoomOnVoting() && !room.isGameStarted() && !room.isRoomFull()) {
                    lobby[roomIndex].addPlayer(username);
                    break;
                } else if (nTries == 10)
                    addRoom();
            }
        }
    }

private:
    std::vector<Room> lobby;
    std::unordered_map<int, size_t> idIndexMap;
    std::mt19937 engine;
    void addRoom() {
        lobby.emplace_back();
        auto emplacedRoom = lobby.size() - 1;
        idIndexMap[lobby[emplacedRoom].getID()] = emplacedRoom;
    }
};


#endif //SERVERKURSSWORK_LOBBY_H
