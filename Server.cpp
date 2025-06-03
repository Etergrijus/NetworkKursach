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
        // Принимаем следующее соединение
        //startAccept();
    } else {
        std::cerr << "Accept error: " << error.message() << std::endl;
    }
    // Всегда перезапускаем ожидание новых соединений
    startAccept();
}

void Server::startRead(const std::shared_ptr<tcpAlias::socket> &socket) {
    // Общий буфер для чтения
    auto buffer = std::make_shared<std::array<char8_t, USERNAME_SIZE>>();
    //Читаем данные
    socket->async_read_some(
            // Буфер для чтения данных
            boost::asio::buffer(buffer->data(), buffer->size()),
            [this, socket, buffer]
                    (const boost::system::error_code &error, size_t bytes) mutable {
                handleRead(socket, error, bytes, buffer);
            });
}

void Server::handleRead(const std::shared_ptr<tcpAlias::socket> &socket, const boost::system::error_code &error,
                        size_t bytes, std::shared_ptr<std::array<char8_t, USERNAME_SIZE>> &buffer) {
    if (!error) {
        std::cout << "handleRead: Received " << bytes << " bytes" << std::endl;  // Лог

        std::u8string rawMessage(buffer->data(), bytes);
        Message msg = parseMessage(rawMessage);
        std::cout << "handleRead: Parsed message scenario: " << static_cast<int>(msg.scenario) << std::endl; // Лог
        std::cout << "handleRead: Parsed message data: " << u8StringToString(msg.data) << std::endl; // Лог

        if (msg.scenario != NetworkScenario::UNKNOWN) {
            handleMessage(msg, socket);
        } else {
            std::cerr << "handleRead: Unknown message type" << std::endl;
        }

        startRead(socket);
    } else if (error == boost::asio::error::eof) {
        closeSocket(socket);
    } else {
        std::cerr << "Read error: " << error.message() << std::endl;
        closeSocket(socket);
    }
}

void Server::handleMessage(const Message &message, const std::shared_ptr<tcpAlias::socket> &socket) {
    auto it = handlerImpls.find(message.scenario);
    if (it != handlerImpls.end()) {
        it->second->handleMessage(message, socket, *this);
    } else {
        std::cerr << "handleMessage: No handler for message type" << std::endl;
        //  Отправьте сообщение об ошибке клиенту (если необходимо)
    }
}

void Server::startWrite(const std::shared_ptr<tcpAlias::socket> &socket, const std::string &message) {
    boost::asio::async_write(
            *socket,
            boost::asio::buffer(message),
            [this, socket]
                    (const boost::system::error_code& error, size_t bytes) {
                handleWrite(socket, error);
            });
}

void Server::handleWrite(const std::shared_ptr<tcpAlias::socket> &socket, const boost::system::error_code &error) {
    if (!error) {
        std::cout << "Answer sent" << std::endl;
    } else if (error == boost::asio::error::eof) {
        closeSocket(socket);
    } else {
        std::cerr << "Write error: " << error.message() << std::endl;
        closeSocket(socket);
    }
}

void Server::closeSocket(const std::shared_ptr<tcpAlias::socket> &socket) {
    if (socket && socket->is_open()) {
        boost::system::error_code error;
        std::cout << "Disonnected: " << socket->remote_endpoint().address().to_string() << std::endl;
        socket->shutdown(boost::asio::socket_base::shutdown_both, error);
        socket->close();
    }
}

std::string Server::buildRoomInfo(Room *room) {
    std::stringstream ss;
    ss << CORRECT_ROOM_INFO << " " << room->getID() << " ";

    for (const auto& player : room->getPlayers()) {
        ss << u8StringToString(player) << " ";
    }

    ss << '\n';
    return ss.str();
}

Lobby& Server::getLobby() {
    return lobby;
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
    //handlerImpls[NetworkScenario::DISCONNECT] = std::make_unique<>()
}

Message Server::parseMessage(const std::u8string &rawMessage) {
    Message msg;
    std::string rawMessageASCII = u8StringToString(rawMessage);
    std::stringstream ss(rawMessageASCII);

    std::string scenarioStr;
    ss >> scenarioStr;
    msg.scenario = defineScenario(scenarioStr);

    std::string data;
    std::getline(ss >> std::ws, data);
    msg.data = stringToU8String(data);
    return msg;
}




