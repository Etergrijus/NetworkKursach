#include <iostream>
#include <boost/asio.hpp>

#include "Server.h"

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
