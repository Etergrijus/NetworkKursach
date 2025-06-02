#include <iostream>
#include <memory>

#include <boost/asio.hpp>

#include "Lobby.h"

#define USERNAME_SIZE 64
#define TRUE_SERVER_ANSWER "Hello\n"

using ioContextAlias = boost::asio::io_context;
using tcpAlias = boost::asio::ip::tcp;

std::string u8string_to_string(const std::u8string& u8str) {
    std::string s(u8str.begin(), u8str.end());
    s.erase(std::remove(s.begin(), s.end(), '\n'), s.cend());
    return s;
}

class Server {
public:
    Server(ioContextAlias &ioContext, int port) :
            _ioContext(ioContext),
            _acceptor(ioContext, tcpAlias::endpoint(tcpAlias::v4(), port)) {
        startAccept();
    }

private:
        void startAccept() {
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

    void handleAccept(const std::shared_ptr<tcpAlias::socket>& socket,
                      const boost::system::error_code &error) {
        if (!error) {
            std::cout << "Connected " << socket->remote_endpoint().address().to_string() << '\n';
            // Начинаем чтение данных от клиента
            startRead(socket);
            // Принимаем следующее соединение
            startAccept();
        } else {
            std::cerr << "Accept error: " << error.message() << std::endl;
        }
        // Всегда перезапускаем ожидание новых соединений
        //startAccept();
    }

    void startRead(const std::shared_ptr<tcpAlias::socket>& socket) {
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

    void handleRead(const std::shared_ptr<tcpAlias::socket>& socket,
                    const boost::system::error_code &error,
                    size_t bytes, std::shared_ptr<std::array<char8_t, USERNAME_SIZE>>& buffer) {
        if (!error) {
            std::u8string username(buffer->data(), bytes);
            std::cout << "Player " << u8string_to_string(username) << " knocks on the door" << std::endl;
            lobby.findRoom(username);

            startWrite(socket, TRUE_SERVER_ANSWER);
            startRead(socket);
        } else if (error == boost::asio::error::eof) {
            closeSocket(socket);
        } else {
            std::cerr << "Read error: " << error.message() << std::endl;
            closeSocket(socket);
        }
    }

    void startWrite(const std::shared_ptr<tcpAlias::socket>& socket,
                    const std::string& message) {
        boost::asio::async_write(
                *socket,
                boost::asio::buffer(message),
                [this, socket]
                (const boost::system::error_code& error, size_t bytes) {
                    handleWrite(socket, error);
                });
    }

    void handleWrite(const std::shared_ptr<tcpAlias::socket>& socket,
                     const boost::system::error_code& error) {
        if (!error) {
            std::cout << "Answer sent\n";
            startRead(socket);
        } else if (error == boost::asio::error::eof) {
            closeSocket(socket);
        } else {
            std::cerr << "Write error: " << error.message() << std::endl;
            closeSocket(socket);
        }
    }

    void closeSocket(const std::shared_ptr<tcpAlias::socket>& socket) {
        if (socket && socket->is_open()) {
            boost::system::error_code error;
            std::cout << "Disonnected: " << socket->remote_endpoint().address().to_string() << std::endl;
            socket->shutdown(boost::asio::socket_base::shutdown_both, error);
            socket->close();
        }
    }


    ioContextAlias &_ioContext;
    tcpAlias::acceptor _acceptor;

    Lobby lobby;
};

int main() {
    SetConsoleOutputCP(CP_UTF8);

    try {
        ioContextAlias ioContext;
        int port = 12345;
        Server server(ioContext, port);
        ioContext.run();
    } catch (std::exception &e) {
        std::cerr << e.what() << std::endl;
    }

    return 0;
}
