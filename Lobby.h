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

    Room *findRoom(const std::u8string &username) {
        Room *room;

        if (lobby.empty()) {
            room = addRoom(username);
            return room;
        } else {
            std::uniform_int_distribution<size_t> dist(0, lobby.size() - 1);
            for (size_t nTries = 0; nTries <= 10; nTries++) {
                size_t roomIndex = dist(engine);
                room = &lobby[roomIndex];
                if (!room->isRoomOnVoting() && !room->isGameStarted() && !room->isRoomFull()) {
                    lobby[roomIndex].addPlayer(username);
                    return room->get();
                } else if (nTries == 10) {
                    room = addRoom(username);
                    return room;
                }
            }
        }
    }

private:
    std::vector<Room> lobby;
    std::unordered_map<int, size_t> idIndexMap;
    std::mt19937 engine;

    Room *addRoom(const std::u8string &username) {
        lobby.emplace_back();
        auto emplacedRoomIndex = lobby.size() - 1;
        idIndexMap[lobby[emplacedRoomIndex].getID()] = emplacedRoomIndex;

        lobby.back().addPlayer(username);
        return lobby.back().get();
    }
};


#endif //SERVERKURSSWORK_LOBBY_H
