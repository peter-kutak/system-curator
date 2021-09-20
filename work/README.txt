apt install libsystemd-dev

gcc $(pkg-config --cflags libsystemd) foo.c $(pkg-config --libs libsystemd)


