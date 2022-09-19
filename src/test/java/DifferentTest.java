import org.junit.jupiter.api.Test;
import utils.InternetAvailabilityChecker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DifferentTest {

    @Test
    void shouldToCheckInternetAvailableTest() throws IOException {
        try (Socket socket = new Socket()) {
            String hostName = "google.com";
            int port = 80;
            if (InternetAvailabilityChecker.isInternetAvailable()) {
                assertDoesNotThrow(() -> socket.connect(new InetSocketAddress(hostName, port), 3000),
                        "Выброшено исключение что нет подключения к интернету");
            } else {
                assertThrows(UnknownHostException.class,
                        () -> socket.connect(new InetSocketAddress(hostName, port), 3000),
                        "Не выброшено исключение о том, что нет подключения к интернету");
            }
        }
    }

}
