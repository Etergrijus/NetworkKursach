#ifndef SERVERKURSSWORK_LOBBY_H
#define SERVERKURSSWORK_LOBBY_H

#include <vector>
#include <string>
#include <unordered_map>
#include <random>

#include "Room.h"

class Lobby {
public:
    Lobby() {
        lobby.reserve(5);
        engine.seed(std::time(nullptr));
    }

    Room *findRoom(const std::u8string &username, const std::shared_ptr<tcpAlias::socket> &socket) {
        Room *room;

        if (lobby.empty()) {
            room = addRoom();
            room->addPlayer(username, socket);
            return room;
        } else {
            std::uniform_int_distribution<size_t> dist(0, lobby.size() - 1);
            for (size_t nTries = 0; nTries <= 10; nTries++) {
                size_t roomIndex = dist(engine);
                room = &lobby[roomIndex];
                if (!room->isRoomOnVoting() && !room->isGameStarted() && !room->isRoomFull()) {
                    lobby[roomIndex].addPlayer(username, socket);
                    return room->get();
                } else if (nTries == 10) {
                    room = addRoom();
                    room->addPlayer(username, socket);
                    return room;
                }
            }
        }

        return nullptr;
    }

    //Для тех, кто решил сменить комнату
    Room *findRoomForDenialist(const int exRoomId,
                               const std::u8string &username, const std::shared_ptr<tcpAlias::socket> &socket) {
        Room *room;
        std::uniform_int_distribution<size_t> dist(0, lobby.size() - 1);
        for (size_t nTries = 0; nTries <= 10; nTries++) {
            size_t roomIndex = dist(engine);
            room = &lobby[roomIndex];
            if (room->getID() != exRoomId && !room->isRoomOnVoting()
                && !room->isGameStarted() && !room->isRoomFull()) {
                lobby[roomIndex].addPlayer(username, socket);
                return room->get();
            } else if (nTries == 10) {
                room = addRoom();
                room->addPlayer(username, socket);
                return room;
            }
        }
    }

    Room *getRoomById(const int id) {
        auto it = idIndexMap.find(id);
        if (it != idIndexMap.end())
            return lobby[idIndexMap[id]].get();
        else
            return nullptr;
    }

private:
    std::vector<Room> lobby;
    std::unordered_map<int, size_t> idIndexMap;
    std::mt19937 engine;

    Room *addRoom() {
        lobby.emplace_back();
        auto emplacedRoomIndex = lobby.size() - 1;
        idIndexMap[lobby[emplacedRoomIndex].getForwardID()] = emplacedRoomIndex;

        return lobby.back().get();
    }
};


#endif //SERVERKURSSWORK_LOBBY_H
