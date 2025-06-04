#include <algorithm>

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

void DisconnectionNetworkHandler::handleMessage(const Message &message,
                                                const std::shared_ptr<tcpAlias::socket> &socket,
                                                Server &server) {
    std::cout << "Received: " << Server::u8StringToString(message.data) << std::endl;

    std::string dataStr = Server::u8StringToString(message.data);
    std::istringstream iss(dataStr);
    int roomId;
    iss >> roomId;
    std::string username;
    iss >> username;

   /* auto it = message.data.begin();
    std::u8string idStr(it, it = std::find(it, message.data.end(), ' '));
    std::cout << Server::u8StringToString(idStr) << std::endl;
    auto roomId = std::stoi(Server::u8StringToString(idStr));*/
    //Игрок выходит до того, как найти комнату
    if (roomId == -1) {
        Server::closeSocket(socket);
        return;
    }

/*    it++;
    std::u8string username(it, message.data.end());*/

    auto room = server.getLobby().getRoomById(roomId);
    if (room) {
        room->exitFromRoom(Server::stringToU8String(username));
        server.startWrite(socket, DISCONNECT_OK);
        Server::closeSocket(socket);
    } else
        std::cerr << "Incorrect id" << std::endl;
}

