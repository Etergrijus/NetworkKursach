#include <boost/asio.hpp>

#include "handlers.h"
#include "Server.h"

NetworkScenario defineScenario(const std::string& str) {
    if (str == CONNECT_STR)
        return NetworkScenario::CONNECT;
    if (str == DISCONNECT_STR)
        return NetworkScenario::DISCONNECT;
    else
        return NetworkScenario::UNKNOWN;
}

Message::Message() = default;

Message::Message(NetworkScenario type, std::u8string str) : scenario(type), data(std::move(str)) {}

NetworkHandler::~NetworkHandler() noexcept = default;

void ConnectionNetworkHandler::handleMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket,
                                             Server &server) {
    std::u8string username = message.data;
    std::cout << "Player " << Server::u8StringToString(message.data) << " knocks on the door" << std::endl;

    //Отправляем клиенту "привет", чтобы он понимал,
    //что подключился к серверу
    server.startWrite(socket, TRUE_SERVER_ANSWER);

    auto room = server.getLobby().findRoom(username);
    if (room) {
        auto roomInfo = Server::buildRoomInfo(room);
        std::cout << "Room info: " << roomInfo << std::endl; // Логирование
        server.startWrite(socket, roomInfo);
        std::cout << "ROOM_INFO sent." << std::endl;
    }
}

