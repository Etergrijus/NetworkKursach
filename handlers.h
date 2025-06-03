#ifndef SERVERKURSSWORK_HANDLERS_H
#define SERVERKURSSWORK_HANDLERS_H

#include <string>
#include <memory>

#include <boost/asio.hpp>
class Server;

enum class NetworkScenario {
    UNKNOWN,
    CONNECT,
    DISCONNECT
};

#define CONNECT_STR "CONNECT"
#define DISCONNECT_STR "DISCONNECT"

NetworkScenario defineScenario(const std::string& str);

struct Message {
    NetworkScenario scenario;
    std::u8string data;

    Message();
    Message(NetworkScenario type, std::u8string str);
};

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


#endif //SERVERKURSSWORK_HANDLERS_H
