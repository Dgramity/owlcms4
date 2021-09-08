/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServletRequest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class IPInterfaceUtils {

    final private Logger logger = (Logger) LoggerFactory.getLogger(IPInterfaceUtils.class);
    {
        logger.setLevel(Level.INFO);
    }

    ArrayList<String> wired = new ArrayList<>();
    ArrayList<String> recommended = new ArrayList<>();
    ArrayList<String> wireless = new ArrayList<>();
    ArrayList<String> loopback = new ArrayList<>();

    private boolean local;

    /**
     * Try to guess URLs that can reach the system.
     *
     * The browser on the master laptop most likely uses "localhost" in its URL. We can't know which of its available IP
     * addresses can actually reach the application. We scan the network addresses, and try the URLs one by one, listing
     * wired interfaces first, and wireless interfaces second (in as much as we can guess).
     *
     * We rely on the URL used to reach the "about" screen to know how the application is named, what port is used, and
     * which protocol works.
     *
     * @return HTML ("a" tags) for the various URLs that appear to work.
     */
    public IPInterfaceUtils() {

        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        Map<String, String> headerMap = getRequestHeadersInMap(request);

        String prefix = "/";
        String targetFile = "sounds/timeOver.mp3";
        checkTargetFileOk(prefix, targetFile);

        String protocol = URLUtils.getScheme(request);
        int requestPort = URLUtils.getServerPort(request);
        String server = URLUtils.getServerName(request);
        String siteString = request.getRequestURI();
        String requestURL = request.getRequestURL().toString();
        String absoluteURL = URLUtils.buildAbsoluteURL(request, null);
//        logger.debug("absolute URL {}", absoluteURL);

        local = isLocalAddress(server) || isLoopbackAddress(server);
//        logger.debug("request {} isLocal: {}", requestURL, local);

        if (!local) {
            // a name was used. this is probably the best option.
            if (absoluteURL.endsWith("/")) {
                absoluteURL = requestURL.substring(0, requestURL.length() - 1);
            }
            recommended.add(absoluteURL);
            // if we are not on the cloud, we try to get a numerical address anyway.
            String forward = headerMap.get("x-forwarded-for");
            if (forward != null) {
                logger.debug("forwarding for {}, proxied, ip address would be meaningless", forward);
                return;
            } else {
                logger.debug("no x-forwarded-for, local machine with host name");
            }
        }

        String ip;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (// iface.isLoopback() ||
                !iface.isUp()) {
                    continue;
                }

                String displayName = iface.getDisplayName();
                String ifaceName = displayName.toLowerCase();

                // filter out interfaces to virtual machines
                if (!virtual(ifaceName)) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        ip = addr.getHostAddress();
                        boolean ipV4 = addr.getAddress().length == 4;
                        if (ipV4) {
                            logger.debug("address:{} local:{} ipv4:{} interface:{}", ip, addr.isSiteLocalAddress(),
                                    ipV4, ifaceName);
                            // try reaching the current IP address with the known protocol, port and site.
                            testIP(protocol, requestPort, siteString, targetFile, ip, ifaceName);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            logger.error(LoggerUtils.stackTrace(e));
        }
        logger.trace("wired = {} {}", wired, wired.size());
        logger.trace("wireless = {} {}", wireless, wireless.size());
    }

    /**
     * @return the loopback
     */
    public ArrayList<String> getLocalUrl() {
        return loopback;
    }

    /**
     * @return the external (non-local) url used to get to the site.
     */
    public ArrayList<String> getRecommended() {
        return recommended;
    }

    public Map<String, String> getRequestHeadersInMap(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        String remoteAddr = request.getRemoteAddr();
        logger.debug("remoteAddr: {}", remoteAddr);
        result.put("remoteAddr", remoteAddr);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement().toLowerCase();
            if (key.equals("x-forwarded-for") || key.equals("host")) {
                String value = request.getHeader(key);
                result.put(key, value);
                logger.debug(key + ": " + value);
            }
        }

        return result;
    }

    /**
     * @return the wired urls
     */
    public ArrayList<String> getWired() {
        return wired;
    }

    /**
     * @return the wireless urls
     */
    public ArrayList<String> getWireless() {
        return wireless;
    }

    private void checkTargetFileOk(String prefix, String targetFile) {
        InputStream targetResource = ResourceWalker.getResourceAsStream(prefix + targetFile); // $NON-NLS-1$
        if (targetResource == null) {
            throw new RuntimeException("test resource not found " + targetFile);
        }
    }

    /**
     * @param serverString
     * @return true if address on a local network (not routed to the internet, not a loopback)
     */
    private boolean isLocalAddress(String serverString) {
        boolean isLocal = false;
        if (serverString.startsWith("10.") || serverString.startsWith("192.168")) {
            isLocal = true;
        } else if (serverString.startsWith("172.")) {
            serverString = serverString.substring(4);
            int sub = serverString.indexOf(".");
            if (sub == -1) {
                isLocal = false;
            } else {
                try {
                    int subnet = Integer.parseInt(serverString.substring(0, sub));
                    isLocal = subnet >= 16 && subnet <= 31;
                } catch (NumberFormatException e) {
                    isLocal = false;
                }
            }

        } else {
            isLocal = false;
        }
        return isLocal;
    }

    private boolean isLoopbackAddress(String serverString) {
        return (serverString.toLowerCase().startsWith("localhost") || serverString.startsWith("127.0.0"));
    }

    private void testIP(String protocol, int requestPort, String uri, String targetFile, String ip, String ifaceName) {
        try {
            URL siteURL = new URL(protocol, ip, requestPort, uri);
            String siteExternalForm = siteURL.toExternalForm();

            // use a file inside the site to avoid triggering a loop if called on home page
            URL testingURL = new URL(protocol, ip, requestPort, uri + targetFile);
            String testingExternalForm = testingURL.toExternalForm();

            HttpURLConnection huc = (HttpURLConnection) testingURL.openConnection();
            huc.setRequestMethod("GET");
            huc.connect();
            int response = huc.getResponseCode();

            siteExternalForm = URLUtils.cleanURL(siteURL, siteExternalForm);

            if (response != 200) {
                logger.debug("{} not reachable: {}", testingExternalForm, response);
            } else {
                logger.debug("{} OK: {}", testingURL, ifaceName);
                if (isLoopbackAddress(ip)) {
                    loopback.add(siteExternalForm);
                } else if (ifaceName.contains("wireless")) {
                    wireless.add(siteExternalForm);
                } else {
                    wired.add(siteExternalForm);
                }
            }
        } catch (Exception e) {
            logger.error(LoggerUtils.stackTrace(e));
        }
    }

    private boolean virtual(String ifaceName) {
        return ifaceName.contains("virtual");
    }

}
