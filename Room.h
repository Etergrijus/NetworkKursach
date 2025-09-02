#ifndef SERVERKURSSWORK_ROOM_H
#define SERVERKURSSWORK_ROOM_H

#include <string>
#include <vector>
#include <algorithm>
#include <map>

#include <boost/asio.hpp>

using tcpAlias = boost::asio::ip::tcp;

struct Player {
    std::u8string username;
    std::shared_ptr<tcpAlias::socket> socket;

    Player() : socket(nullptr) {};

    Player(std::u8string _username, const std::shared_ptr<tcpAlias::socket> &sock) :
            socket(sock), username(std::move(_username)) {};
};

class Room {
public:
    Room() {
        players.resize(maxPlayers);
        isVotedNow = false;
        isStarted = false;
        nPlayers = 0;

        forwardId++;
        thisRoomId = forwardId;
    }

    Room *get() {
        return this;
    }

    static int getForwardID() {
        return forwardId;
    }

    int getID() {
        return thisRoomId;
    }

    const std::vector<Player>& getPlayers() const {
        return players;
    }

    void setPlayers(const int playersInGame) {
        players.resize(playersInGame);
    }

    void addPlayer(const std::u8string &username, const std::shared_ptr<tcpAlias::socket> &socket) {
        nPlayers++;
        players[nPlayers - 1] = Player(username, socket);
    }

    void exitFromRoom(const std::u8string &username) {
        nPlayers--;
        players.erase(std::remove_if(players.begin(), players.end(),
                                     [username](Player &p) { return p.username == username; }),
                      players.end());
        players.resize(maxPlayers);
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

    void startVoting() {
        isVotedNow = true;
    }

    void interruptVoting() {
        nVotedPlayers = 0;
        nVoteAcceptedPlayer = 0;
        isVotedNow = false;
    }

    //Обрабатывает игрока, который проголосовал
    //ЗА начало игры
    void accepted() {
        nVoteAcceptedPlayer++;
        nVotedPlayers++;
    }

    //Обрабатывает игрока, который проголосовал
    //ПРОТИВ начала игры
    void declined() {
        nVotedPlayers++;
    }

    bool isEndVoting() const {
        return nVotedPlayers == nPlayers;
    }

    //Возвращает true, если игроки проголосовали за начало игры
    //и ложь в противном случае
    bool endVoting() {
        auto resVal = nPlayers > 1 && nVotedPlayers == nVoteAcceptedPlayer;
        nVotedPlayers = 0;
        nVoteAcceptedPlayer = 0;
        isVotedNow = false;
        return resVal;
    }

    void setGameStarted(bool started) {
        isStarted = started;
    }

    //Старт игры
    //Обрабока голосования

private:
    std::vector<Player> players;
    bool isVotedNow;
    bool isStarted;
    int nPlayers;
    //ID последней созданной комнаты, "наконечной", если рассматривать
    //лобби как вектор
    inline static int forwardId = 0;
    //ID конкретной (данной) комнаты
    int thisRoomId;

    int nVoteAcceptedPlayer = 0;
    int nVotedPlayers = 0;

    const int maxPlayers = 4;

};

#endif //SERVERKURSSWORK_ROOM_H
