cmake_minimum_required(VERSION 3.24)
project(ServerKurssWork)

set(CMAKE_CXX_STANDARD 20)

set(BOOST_INCLUDE_DIR "D:\\dev\\boost\\boost_1_88_0") # Проверьте этот путь!
include_directories(${BOOST_INCLUDE_DIR})

# find_package(Boost REQUIRED)

link_libraries(ws2_32 wsock32)

add_executable(ServerKurssWork main.cpp Lobby.h Room.h) # Добавьте все ваши .cpp файлы
