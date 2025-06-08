#ifndef SERVERKURSSWORK_HANDLERS_H
#define SERVERKURSSWORK_HANDLERS_H

#include <string>
#include <memory>

#include <boost/asio.hpp>
class Server;

//4 основных сетевых взаимодействия ("сценария"):
// -> Подключение - работает с лобби
// -> Отключение - работает с комнатой
// -> Смена комнаты (или "отказ" от игры в данной комнате) - работает с лобби
// -> Голосование о начале игры ("начало голосования") - работает с комнатой
enum class NetworkScenario {
    UNKNOWN,
    CONNECT,
    DISCONNECT,
    DENIAL,
    START_VOTING
};

#define CONNECT_STR "CONNECT"
#define DISCONNECT_STR "DISCONNECT"
#define DENIAL_STR "DENIAL"
#define START_VOTING_STR "START_VOTING"

//Обработка клиентом событий других клиентов - дополнительные сетевые взаимодействия
// -> Присоединение
// -> Отсоединенение
//Работают с комнатой, помогают клиенту отображать правильный UI
#define JOIN_MESSAGE "JOIN"
#define LEAVE_MESSAGE "LEAVE"

enum class VotingAnswer {
    UNKNOWN,
    ACCEPT,
    DECLINE
};

#define ACCEPT_STR "ACCEPT"
#define DECLINE_STR "DECLINE"

enum class GameMessage {
    UNKNOWN,
    MOVE
};

#define MOVE_STR "MOVE"

std::variant<NetworkScenario, VotingAnswer, GameMessage> defineScenario(const std::string& str);

struct Message {
    NetworkScenario scenario;
    VotingAnswer voteScenario;
    GameMessage gameScenario;
    std::u8string data;

    Message();
    Message(NetworkScenario type, std::u8string str);
};

//Каждому сценарию делаем свой хендлер-класс,
//обязательно регистрируем его в registerHandlerImpls() сервера (Server.cpp)
class NetworkHandler {
public:
    virtual ~NetworkHandler() noexcept;
    virtual void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                               Server& server) = 0;
};

class ConnectionNetworkHandler : public NetworkHandler {
public:
    void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                       Server& server) override;
};

class DisconnectionNetworkHandler : public NetworkHandler {
public:
    void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                       Server& server) override;
};

class DenialNetworkHandler : public NetworkHandler {
public:
    void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                       Server& server) override;
};

class StartVotingHandler : public NetworkHandler {
    void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                       Server& server) override;
};

class VotingHandler : public NetworkHandler {
    void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                       Server& server) override;
};

class GameHandler : public NetworkHandler {
    void handleMessage(const Message& message, const std::shared_ptr<boost::asio::ip::tcp::socket>& socket,
                       Server& server) override;
};

#endif //SERVERKURSSWORK_HANDLERS_H
