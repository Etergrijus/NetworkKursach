#include "Server.h"
#include "handlers.h"

Server::Server(ioContextAlias &ioContext, int port) :
        _ioContext(ioContext),
        _acceptor(ioContext, tcpAlias::endpoint(tcpAlias::v4(), port)) {
    registerHandlerImpls();
    startAccept();
}

void Server::startAccept() {
    auto socket = std::make_shared<tcpAlias::socket>(_ioContext);
    std::cout << "Server awaiting..." << std::endl;
    _acceptor.async_accept(
            // Указываем socket, который будет использоваться для принятия соединения.
            *socket,
            // Лямбда-функция для обработки принятия соединения.
            [this, socket]
                    (const boost::system::error_code &error) {
                handleAccept(socket, error);
            });
}

void Server::handleAccept(const std::shared_ptr<tcpAlias::socket> &socket, const boost::system::error_code &error) {
    if (!error) {
        std::cout << "Connected " << socket->remote_endpoint().address().to_string() << '\n';
        // Начинаем чтение данных от клиента
        startRead(socket);
    } else {
        std::cerr << "Accept error: " << error.message() << std::endl;
    }
    // Всегда перезапускаем ожидание новых соединений
    startAccept();
}

void Server::startRead(const std::shared_ptr<tcpAlias::socket> &socket) {
    if (socket->is_open()) {
        // Общий буфер для чтения
        auto buffer = std::make_shared<std::array<char8_t, BUFFER_SIZE>>();
        //Читаем данные
        socket->async_read_some(
                // Буфер для чтения данных
                boost::asio::buffer(buffer->data(), buffer->size()),
                [this, socket, buffer]
                        (const boost::system::error_code &error, size_t bytes) mutable {
                    handleRead(socket, error, bytes, buffer);
                });
    }
}

void Server::handleRead(const std::shared_ptr<tcpAlias::socket> &socket, const boost::system::error_code &error,
                        size_t bytes, std::shared_ptr<std::array<char8_t, BUFFER_SIZE>> &buffer) {
    if (!error) {
        std::u8string rawMessage(buffer->data(), bytes);
        Message msg = parseMessage(rawMessage);

        //Неизвестные значения пишутся в scenario, отлавливаем их оттуда
        if (msg.scenario != NetworkScenario::UNKNOWN) {
            handleMessage(msg, socket);
        } else {
            std::cerr << "handleRead: Unknown message type" << std::endl;
        }

        startRead(socket);
    } else if (error == boost::asio::error::eof) {
        //closeSocket(socket);
    } else {
        std::cerr << "Read error: " << error.what() << std::endl;
        closeSocket(socket);
    }
}

void Server::handleMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket) {
    auto it = handlerImpls.find(message.scenario);
    if (it != handlerImpls.end()) {
        it->second->handleNetworkMessage(message, socket, *this);
        return;
    }

    it = handlerImpls.find(message.voteScenario);
    if (it != handlerImpls.end()) {
        it->second->handleNetworkMessage(message, socket, *this);
        return;
    }

/*
    it = handlerImpls.find(message.gameScenario);
    if (it != handlerImpls.end()) {
        it->second->handleNetworkMessage(message, socket, *this);
        return;
    }
*/

    std::cerr << "handleNetworkMessage: No handler for message type" << std::endl;
}

void Server::startWrite(const std::shared_ptr<tcpAlias::socket> &socket, const std::string &message) {
    if (socket && socket->is_open()) {
        boost::asio::async_write(
                *socket,
                boost::asio::buffer(message),
                [this, socket]
                        (const boost::system::error_code &error, size_t bytes) {
                    handleWrite(socket, error);
                });
    }
}

void Server::handleWrite(const std::shared_ptr<tcpAlias::socket> &socket, const boost::system::error_code &error) {
    if (!error) {
        //std::cout << "Answer sent" << std::endl;
    } else if (error == boost::asio::error::eof) {
        //closeSocket(socket);
    } else {
        std::cerr << "Write error: " << error.message() << std::endl;
        closeSocket(socket);
    }
}

void Server::closeSocket(const std::shared_ptr<tcpAlias::socket> &socket) {
    if (socket && socket->is_open()) {
        boost::system::error_code error;
        std::cout << "Disconnected: " << socket->remote_endpoint().address().to_string() << std::endl;
        socket->cancel(error);
        socket->shutdown(boost::asio::socket_base::shutdown_both, error);
        socket->close();
    }
}

std::string Server::buildRoomInfo(Room *room) {
    std::stringstream ss;
    ss << CORRECT_ROOM_INFO << " " << room->getID() << " ";

    for (const auto& player : room->getPlayers()) {
        ss << u8StringToString(player.username) << " ";
    }

    auto resVal = ss.str();
    resVal.pop_back();
    resVal += "\n";
    return resVal;
}

Lobby& Server::getLobby() {
    return lobby;
}

void Server::allPlayersSending(Room* room, const std::string &message, const std::u8string& playerName,
                               bool isEqualCheckNeed) {
    for (const auto& player: room->getPlayers()) {
        if (player.socket && !player.socket->is_open()) {
            room->exitFromRoom(player.username);
            allPlayersSending(room, LEAVE_MESSAGE, player.username);
        }
    }

    for (const auto& player: room->getPlayers()) {
        if (!isEqualCheckNeed || player.username != playerName) {
            startWrite(player.socket, message + " " + u8StringToString(playerName) + "\n");
        }
    }
}

void Server::startGame(Room *room) {
/*    activeGames[room->getID()] = std::make_unique<Game>(*room);
    activeGames[room->getID()]->startGame();*/

    //Останавливает callback'и чтения в этом классе для сокетов игроков
    //в начинающейся игры. Передаём управление сетью игровому серверу
    for (auto &player: room->getPlayers())
        player.socket->cancel();

    activeGames[room->getID()] = std::make_unique<GameServer>(*room);
}

std::unordered_map<int, std::unique_ptr<GameServer>>& Server::getGames() {
    return activeGames;
}

std::string Server::u8StringToString(const std::u8string &u8str) {
    std::string s(u8str.begin(), u8str.end());
    s.erase(std::remove(s.begin(), s.end(), '\n'), s.cend());
    return s;
}

std::u8string Server::stringToU8String(const std::string &str) {
    std::u8string result;
    result.reserve(str.length());  // Reserve space for efficiency
    for (char c : str) {
        result += static_cast<char8_t>(c);
    }
    return result;
}

void Server::registerHandlerImpls() {
    handlerImpls[NetworkScenario::CONNECT] = std::make_unique<ConnectionNetworkHandler>();
    handlerImpls[NetworkScenario::DISCONNECT] = std::make_unique<DisconnectionNetworkHandler>();
    handlerImpls[NetworkScenario::DENIAL] = std::make_unique<DenialNetworkHandler>();
    handlerImpls[NetworkScenario::START_VOTING] = std::make_unique<StartVotingHandler>();

    handlerImpls[VotingAnswer::ACCEPT] = std::make_unique<VotingHandler>();
    handlerImpls[VotingAnswer::DECLINE] = std::make_unique<VotingHandler>();
}

Message Server::parseMessage(const std::u8string &rawMessage) {
    Message msg;
    std::string rawMessageASCII = u8StringToString(rawMessage);
    std::stringstream ss(rawMessageASCII);

    std::string scenarioStr;
    ss >> scenarioStr;

    auto scenario = defineScenario(scenarioStr);
    if (std::holds_alternative<NetworkScenario>(scenario))
        msg.scenario = std::get<NetworkScenario>(scenario);
    else if (std::holds_alternative<VotingAnswer>(scenario))
        msg.voteScenario = std::get<VotingAnswer>(scenario);
    else {
        std::cerr << "Unknown scenario, std::variant failed" << std::endl;
        throw std::runtime_error("std::variant bad");
    }

    std::string data;
    std::getline(ss >> std::ws, data);
    msg.data = stringToU8String(data);
    return msg;
}




