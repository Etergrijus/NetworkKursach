#include <algorithm>

#include <boost/asio.hpp>

#include "handlers.h"
#include "Server.h"

std::variant<NetworkScenario, VotingAnswer> defineScenario(const std::string &str) {
    if (str == CONNECT_STR)
        return NetworkScenario::CONNECT;
    if (str == DISCONNECT_STR)
        return NetworkScenario::DISCONNECT;
    if (str == DENIAL_STR)
        return NetworkScenario::DENIAL;
    if (str == START_VOTING_STR)
        return NetworkScenario::START_VOTING;
    if (str == ACCEPT_STR)
        return VotingAnswer::ACCEPT;
    if (str == DECLINE_STR)
        return VotingAnswer::DECLINE;

    return NetworkScenario::UNKNOWN;
}

//Возвращает кортеж с двумя значениями:
// - id - int
// - username - string
//Гарантируется, что больше параметров нет, иначе данная функция
//выкинет logic_error
std::tuple<int, std::string> parseMessageData(const std::u8string &data) {
    std::string dataStr = Server::u8StringToString(data);
    std::istringstream iss(dataStr);
    int roomId;
    iss >> roomId;
    std::string username;
    iss >> username;

    if (!iss.eof())
        throw std::logic_error("Bad data message format");

    return std::make_tuple(roomId, username);
}

void votingInterrupt(Room* room, Server& server) {
    room->interruptVoting();
    server.allPlayersSending(room, NOT_START_GAME, u8"", false);
}

Message::Message() = default;

Message::Message(NetworkScenario type, std::u8string str) : scenario(type), data(std::move(str)) {}

NetworkHandler::~NetworkHandler() noexcept = default;

void ConnectionNetworkHandler::handleNetworkMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket,
                                                    Server &server) {
    std::u8string username = message.data;
    std::cout << "Player " << Server::u8StringToString(message.data) << " knocks on the door" << std::endl;

    //Отправляем клиенту "привет", чтобы он понимал,
    //что подключился к серверу
    server.startWrite(socket, TRUE_SERVER_ANSWER);

    auto room = server.getLobby().findRoom(username, socket);
    if (room) {
        server.allPlayersSending(room, JOIN_MESSAGE, username);

        auto roomInfo = Server::buildRoomInfo(room);
        std::cout << "Room info: " << roomInfo << std::endl; // Логирование
        server.startWrite(socket, roomInfo);
        std::cout << "ROOM_INFO sent." << std::endl;
    }
}

void DisconnectionNetworkHandler::handleNetworkMessage(const Message &message,
                                                       const std::shared_ptr<tcpAlias::socket> &socket,
                                                       Server &server) {
    auto data = parseMessageData(message.data);
    //Игрок выходит до того, как найти комнату
    if (std::get<0>(data) == -1) {
        std::cout << "impatient" << std::endl;
        Server::closeSocket(socket);
        return;
    }

    auto room = server.getLobby().getRoomById(std::get<0>(data));
    if (room) {
        auto usernameUtf = Server::stringToU8String(std::get<1>(data));
        room->exitFromRoom(usernameUtf);
        server.startWrite(socket, DISCONNECT_OK);
        server.allPlayersSending(room, LEAVE_MESSAGE, usernameUtf);

        //votingInterrupt(room, server);
        Server::closeSocket(socket);
    } else
        std::cerr << "Incorrect forwardId" << std::endl;
}

void DenialNetworkHandler::handleNetworkMessage(const Message &message,
                                                const std::shared_ptr<tcpAlias::socket> &socket, Server &server) {
    auto data = parseMessageData(message.data);

    server.startWrite(socket, NEW_ROOM_FINDING);
    auto usernameUtf = Server::stringToU8String(std::get<1>(data));
    auto exRoom = server.getLobby().getRoomById(std::get<0>(data));
    //"Отказник" вышел
    exRoom->exitFromRoom(usernameUtf);
    server.allPlayersSending(exRoom, LEAVE_MESSAGE, usernameUtf);
    votingInterrupt(exRoom, server);

    auto room =
            server.getLobby().findRoomForDenialist(std::get<0>(data), usernameUtf, socket);
    if (room) {
        auto roomInfo = Server::buildRoomInfo(room);
        std::cout << "Room info: " << roomInfo << std::endl; // Логирование
        server.startWrite(socket, roomInfo);
        std::cout << "ROOM_INFO sent." << std::endl;
        server.allPlayersSending(room, JOIN_MESSAGE, usernameUtf);
    }
}

void StartVotingHandler::handleNetworkMessage(const Message &message,
                                              const std::shared_ptr<tcpAlias::socket> &socket, Server &server) {
    auto data = parseMessageData(message.data);
    auto usernameUtf = Server::stringToU8String(std::get<1>(data));
    auto room = server.getLobby().getRoomById(std::get<0>(data));

    if (room) {
        server.allPlayersSending(room, NEED_VOTING, usernameUtf);
        std::cout << std::get<1>(data) << " starts the vote" << std::endl;

        room->startVoting();
    } else
        std::cerr << "Incorrect id" << std::endl;
}

void VotingHandler::handleNetworkMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket,
                                         Server &server) {
    auto data = parseMessageData(message.data);
    auto usernameUtf = Server::stringToU8String(std::get<1>(data));
    auto room = server.getLobby().getRoomById(std::get<0>(data));

    if (room) {
        bool accepted = (message.voteScenario == VotingAnswer::ACCEPT);
        if (accepted) {
            std::cout << std::get<1>(data) << " accepted" << std::endl;
            room->accepted();
        } else {
            std::cout << std::get<1>(data) << " declined" << std::endl;
            room->declined();
        }
        //Рассылаем игрокам (кроме проголосовавшего) ответ
        server.allPlayersSending(room, accepted ? ACCEPTED : DECLINED,
                                 usernameUtf, false);

        //Проверяем, окончилось ли голосование
        if (room->isEndVoting()) {
            if (room->endVoting()) {
                server.allPlayersSending(room, START_GAME, u8"", false);
                room->setGameStarted(true); //  Add setter
                server.startGame(room);
                std::cout << "Started the game in room " << std::get<0>(data) << std::endl;
            } else {
                server.allPlayersSending(room, NOT_START_GAME, u8"", false);
                room->setGameStarted(false);
                std::cout << "Declined in room " << std::get<0>(data) << std::endl;
            }
        }
    } else {
        std::cerr << "Incorrect id" << std::endl;
    }
}

/*void GameHandler::handleNetworkMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket,
                                       Server &server) {
    auto roomId = stoi(Server::u8StringToString(message.data));
    auto it = server.getGames().find(roomId);

    if (it != server.getGames().end()) {
        it->second->rollDice();
    }
}*/

