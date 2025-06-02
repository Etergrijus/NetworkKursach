#ifndef SERVERKURSSWORK_ROOM_H
#define SERVERKURSSWORK_ROOM_H

#include <string>
#include <vector>
#include <algorithm>

class Room {
public:
    Room() {
        players.resize(4);
        isVotedNow = false;
        isStarted = false;
        nPlayers = 0;

        id++;
    }

    int getID() const {
        return id;
    }

    void addPlayer(const std::u8string& username) {
        nPlayers++;
        players[nPlayers - 1] = username;
    }

    void exitFromRoom(const std::u8string& username) {
        nPlayers--;
        players.erase(std::remove(players.begin(), players.end(), username),players.end());
    }

    bool isRoomFull() const {
        return nPlayers == maxPlayers;
    }

    bool isRoomOnVoting() const {
        return isVotedNow;
    }

    bool isGameStarted() const {
        return isStarted;
    }

/*    void startVoting() {
        isVotedNow = true;
    }*/

    //Старт игры
    //Обрабока голосования

private:
    std::vector<std::u8string> players;
    bool isVotedNow;
    bool isStarted;
    int nPlayers;
    int id = 0;

    const int maxPlayers = 4;
};


#endif //SERVERKURSSWORK_ROOM_H
