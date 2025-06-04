#ifndef SERVERKURSSWORK_SERVER_H
#define SERVERKURSSWORK_SERVER_H

#include <iostream>
#include <memory>
#include <boost/asio.hpp>

#include "Lobby.h"

#include "handlers.h"

#define BUFFER_SIZE 512
#define TRUE_SERVER_ANSWER "Hello\n"
#define DISCONNECT_OK "Disconnected\n"
#define CORRECT_ROOM_INFO "ROOM_INFO"

using ioContextAlias = boost::asio::io_context;
using tcpAlias = boost::asio::ip::tcp;

enum class NetworkScenario;

class NetworkHandler;

class Server {
public:
    Server(ioContextAlias &ioContext, int port);

    void startAccept();

    void startRead(const std::shared_ptr<tcpAlias::socket>& socket);

    void startWrite(const std::shared_ptr<tcpAlias::socket>& socket,
                    const std::string& message);

    static void closeSocket(const std::shared_ptr<tcpAlias::socket>& socket);

    static std::string buildRoomInfo(Room* room);

    Lobby& getLobby();

    static std::string u8StringToString(const std::u8string& u8str);

    static std::u8string stringToU8String(const std::string &str);

private:
    void handleAccept(const std::shared_ptr<tcpAlias::socket>& socket,
                      const boost::system::error_code &error);

    void handleRead(const std::shared_ptr<tcpAlias::socket>& socket,
                    const boost::system::error_code &error,
                    size_t bytes, std::shared_ptr<std::array<char8_t, BUFFER_SIZE>>& buffer);

    void handleMessage(const Message& message, const std::shared_ptr<tcpAlias::socket>& socket);

    void handleWrite(const std::shared_ptr<tcpAlias::socket>& socket,
                     const boost::system::error_code& error);

    void registerHandlerImpls();

    static Message parseMessage(const std::u8string& rawMessage);


    ioContextAlias &_ioContext;
    tcpAlias::acceptor _acceptor;

    std::unordered_map<NetworkScenario, std::unique_ptr<NetworkHandler>> handlerImpls;

    Lobby lobby;
};


#endif //SERVERKURSSWORK_SERVER_H
