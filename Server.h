#ifndef SERVERKURSSWORK_SERVER_H
#define SERVERKURSSWORK_SERVER_H

#include <iostream>
#include <memory>
#include <boost/asio.hpp>

#include "Lobby.h"

#include "handlers.h"
#include "Game.h"

#define BUFFER_SIZE 512
#define TRUE_SERVER_ANSWER "Hello\n"
#define DISCONNECT_OK "Disconnected\n"
#define NEW_ROOM_FINDING "Finding\n"

//Сообщения, после которых идёт информация - \n НЕ нужен

#define CORRECT_ROOM_INFO "Your_room"
#define NEED_VOTING "Voting_started"
#define ACCEPTED "Accepted"
#define DECLINED "Declined"

#define START_GAME "Game_started"
#define NOT_START_GAME "Game_not_started"

using ioContextAlias = boost::asio::io_context;
using tcpAlias = boost::asio::ip::tcp;

enum class NetworkScenario;

class NetworkHandler;

class Server {
public:
    Server(ioContextAlias &ioContext, int port);

    void startAccept();

    void startRead(const std::shared_ptr<tcpAlias::socket> &socket);

    void startWrite(const std::shared_ptr<tcpAlias::socket> &socket,
                    const std::string &message);

    static void closeSocket(const std::shared_ptr<tcpAlias::socket> &socket);

    static std::string buildRoomInfo(Room *room);

    Lobby &getLobby();

    void allPlayersSending(Room *room, const std::string &message, const std::u8string &playerName = u8"",
                           bool isEqualCheckNeed = true);

    void startGame(Room *room);

    std::unordered_map<int, std::unique_ptr<Game>>& getGames();

    static std::string u8StringToString(const std::u8string &u8str);

    static std::u8string stringToU8String(const std::string &str);

private:
    void handleAccept(const std::shared_ptr<tcpAlias::socket> &socket,
                      const boost::system::error_code &error);

    void handleRead(const std::shared_ptr<tcpAlias::socket> &socket,
                    const boost::system::error_code &error,
                    size_t bytes, std::shared_ptr<std::array<char8_t, BUFFER_SIZE>> &buffer);

    void handleMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket);

    void handleWrite(const std::shared_ptr<tcpAlias::socket> &socket,
                     const boost::system::error_code &error);

    void registerHandlerImpls();

    static Message parseMessage(const std::u8string &rawMessage);

    ioContextAlias &_ioContext;
    tcpAlias::acceptor _acceptor;

    std::unordered_map<std::variant<NetworkScenario, VotingAnswer, GameMessage>,
            std::unique_ptr<NetworkHandler>> handlerImpls;

    std::unordered_map<int, std::unique_ptr<Game>> activeGames;

    Lobby lobby;
};


#endif //SERVERKURSSWORK_SERVER_H
