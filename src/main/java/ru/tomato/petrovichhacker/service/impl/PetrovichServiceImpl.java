package ru.tomato.petrovichhacker.service.impl;

import org.apache.logging.log4j.util.Strings;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.tomato.petrovichhacker.PetrovichHackerUtils;
import ru.tomato.petrovichhacker.bot.PetrovichHackerBot;
import ru.tomato.petrovichhacker.pojo.GoodDetails;
import ru.tomato.petrovichhacker.pojo.User;
import ru.tomato.petrovichhacker.pojo.WarehouseDeliveryDetails;
import ru.tomato.petrovichhacker.service.PetrovichService;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PetrovichServiceImpl implements PetrovichService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PetrovichServiceImpl.class);
    private static final String PETROVICH_AUTH_URL = "https://api.petrovich.ru/api/rest/v1/user/login?embedded=cart,card,profile,is_auth,user_type&city_code=spb&client_id=pet_site";
    private static final String EMAIL = "win311@gmail.com";
    private static final String PASSWORD = "fghjytre32";
    public static final String AVAILABILITY = "Наличие";
    public static final String NON_AVAILABILITY = "Недоступно в этом городе";
    private List<String> goodsIds;
    private RestTemplate restTemplate;
    private PetrovichHackerBot petrovichHackerBot;

    public PetrovichServiceImpl(PetrovichHackerBot petrovichHackerBot, RestTemplate restTemplate) {
        this.petrovichHackerBot = petrovichHackerBot;
        this.restTemplate = restTemplate;
        this.goodsIds = getGoodsIds();
    }

    @Override
    public boolean auth() {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("cookie", getCookieForAuthRequest());
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            User user = new User();
            user.setEmail(EMAIL);
            user.setPassword(PASSWORD);

            HttpEntity<User> httpEntity = new HttpEntity<>(user, httpHeaders);
            ResponseEntity responseEntity = this.restTemplate.exchange(PETROVICH_AUTH_URL, HttpMethod.POST, httpEntity, String.class);
            if (Objects.equals(responseEntity.getStatusCode(), HttpStatus.OK)) {
                return true;
            }
        } catch (HttpClientErrorException httpClientErrorException) {
            LOGGER.error(httpClientErrorException.getMessage());
            if ((Objects.equals(httpClientErrorException.getStatusCode(), HttpStatus.BAD_REQUEST))) {
                return true;
            }
        }
        return false;
    }

    private List<String> getGoodsIds() {
        File file = new File("./goods_ids.config");
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            LOGGER.info(e.getCause().getMessage());
        }

        List<String> idList = new ArrayList<>();
        try {
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (Strings.isNotEmpty(line)) {
                    idList.add(line.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getCause().getMessage());
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                LOGGER.error("Error occurred while file processing. Cause: {}", e.getMessage());
            }
        }
        return idList;
    }

    @Override
    public void checkValues() {
        for (String goodId : goodsIds) {
            Document document = getDocument(PetrovichHackerUtils.getUrl(goodId));
            Element body = document.body();
            Optional<Element> productSidebarContent = Optional.ofNullable(body)
                    .map(e -> getSingleElementByClassName(e, "product-sidebar-content"));

            Element result = productSidebarContent
                    .map(e -> getSingleElementByClassName(e, "remains-card-title"))
                    .map(e -> getSingleElementByTag(e, "h3"))
                    .orElse(null);

            if (result == null) {
                LOGGER.info("[BOOKING]: Good is just available for booking: {}", goodId);
                LOGGER.info(body.html());
                this.petrovichHackerBot.notifyAboutGoodsAvailableForBooking(goodId);
            } else if (Objects.equals(result.text(), AVAILABILITY)) {
                LOGGER.info("[AVAILABLE]: You can buy good {}", goodId);
                GoodDetails goodDetails = getGoodDetails(productSidebarContent);
                LOGGER.info("Good details: {}", goodDetails);
                this.petrovichHackerBot.notifyAboutSuccess(goodId, goodDetails);
            } else if (Objects.equals(result.text(), NON_AVAILABILITY)) {
                LOGGER.info("[ABSENT] Good is absent: {}", goodId);
            } else {
                LOGGER.warn("[STRANGE] Something strange occurred... It is better to check by the link: {}", goodId);
                this.petrovichHackerBot.notifyAboutStranges(goodId);
            }
        }
    }

    private GoodDetails getGoodDetails(Optional<Element> productSidebarContent) {
        GoodDetails goodDetails = null;

        Optional<Elements> supplyWayBlocks = productSidebarContent
                .map(e -> getSingleElementByClassName(e, "pt-w-stretch"))
                .map(e -> e.getElementsByClass("supply-way-block"));

        if (supplyWayBlocks.isPresent()) {
            goodDetails = new GoodDetails();
            String[] timeAndValue = supplyWayBlocks.get().stream()
                    .filter(block -> Objects.equals(getBlockTitle(block), "Доставим"))
                    .map(this::getSingleTitleAndValue)
                    .findFirst()
                    .orElse(null);

            List<WarehouseDeliveryDetails> warehouseDeliveryDetailsList = supplyWayBlocks.get().stream()
                    .filter(block -> Objects.equals(getBlockTitle(block), "Доступно со склада самовывоза"))
                    .map(this::getTitleAndValuePairs)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(titleAndValue -> new WarehouseDeliveryDetails(titleAndValue[0], titleAndValue[1]))
                    .collect(Collectors.toList());

            if (timeAndValue != null) {
                goodDetails.setDeliveryTime(timeAndValue[0]);
                goodDetails.setAvailableForDeliveryAmount(timeAndValue[1]);
            }

            if (!CollectionUtils.isEmpty(warehouseDeliveryDetailsList)) {
                goodDetails.setWarehouseDeliveryDetailsList(warehouseDeliveryDetailsList);
            }
        }
        return goodDetails;
    }

    private String getBlockTitle(Element supplyWayBlock) {
        TextNode textNode = Optional.ofNullable(supplyWayBlock)
                .map(block -> getSingleElementByTag(block, "p"))
                .map(Element::textNodes)
                .map(nodes -> nodes.get(0))
                .orElse(null);

        return textNode != null ? textNode.text() : null;
    }

    private List<String[]> getTitleAndValuePairs(Element element) {
        Elements liElements = Optional.ofNullable(element)
                .map(e -> getSingleElementByTag(e, "ul"))
                .map(e -> e.getElementsByTag("li"))
                .orElseGet(null);

        if (liElements != null) {
            return liElements.stream()
                    .map(e -> getSingleTitleAndValuePair(Optional.ofNullable(e)))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private String[] getSingleTitleAndValue(Element element) {
        Optional<Element> liElement = Optional.ofNullable(element)
                .map(e -> getSingleElementByTag(e, "ul"))
                .map(e -> getSingleElementByTag(e, "li"));
        return getSingleTitleAndValuePair(liElement);
    }

    private String[] getSingleTitleAndValuePair(Optional<Element> titleAndValueElement) {
        String title = titleAndValueElement
                .map(e -> getSingleElementByClassName(e, "title"))
                .map(Element::text)
                .orElseGet(null);

        String value = titleAndValueElement
                .map(e -> getSingleElementByClassName(e, "value"))
                .map(Element::text)
                .orElseGet(null);

        String[] values = new String[2];
        if (Objects.nonNull(title) && Objects.nonNull(value)) {
            values[0] = title;
            values[1] = value;
        }
        return values;
    }

    private Element getSingleElementByClassName(Element parentElement, String className) {
        List<Element> elements = parentElement.getElementsByClass(className);
        if (!CollectionUtils.isEmpty(elements)) {
            return elements.get(0);
        }
        return null;
    }

    private Element getSingleElementByTag(Element parentElement, String tag) {
        List<Element> elements = parentElement.getElementsByTag(tag);
        if (!CollectionUtils.isEmpty(elements)) {
            return elements.get(0);
        }
        return null;
    }

    private Document getDocument(String url) {
        Connection connection = Jsoup.connect(url);
        try {
            return connection.get();
        } catch (IOException e) {
            LOGGER.error("Error occurred during connection to the site. Error cause is: {}", e.getMessage());
        }
        return null;
    }

    private String getCookieForAuthRequest() {
        return "SNK=124; " +
                "u__typeDevice=desktop;" +
                " u__cityCode=spb;" +
                " SIK=fAAAAIJsfnRJO0sQPUQEAA;" +
                " SIV=1;" +
                " C_cEWqnqxXfu5_meMhVqQE7TX46Fw=AAAAAAAACEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA8D8AACApa4jpQc91imh6Z2yKkkyhw-yJkk4;" +
                " tmr_lvid=092b988c549938fe496c2ae4d6bca47e;" +
                " tmr_lvidTS=1630349858863;" +
                " ssaid=2592f230-09c4-11ec-adae-9f5853f5ae20;" +
                " dd_custom.lastViewedProductImages=[];" +
                " _gcl_au=1.1.1370986671.1630349859;" +
                " aplaut_distinct_id=6CITMmKYdeOI;" +
                " _gid=GA1.2.1703264357.1630349863;" +
                " _fbp=fb.1.1630349862716.1326387601;" +
                " _ym_uid=1630349863591549699;" +
                " _ym_d=1630349863;" +
                " popmechanic_sbjs_migrations=popmechanic_1418474375998=1|||1471519752600=1|||1471519752605=1;" +
                " _ym_isad=2;" +
                " rrpvid=625884152718311;" +
                " _hjid=a9a14595-cfdf-4193-aae3-a1ce303d0b60;" +
                " _hjFirstSeen=1;" +
                " _hjAbsoluteSessionInProgress=0;" +
                " rcuid=6072f0b54bb29900010e23e3;" +
                " dd_user.everLoggedIn=true;" +
                " dd__persistedKeys=[\"custom.lastViewedProductImages\",\"user.everLoggedIn\",\"user.email\"];" +
                " dd_user.email=win311@gmail.com;" +
                " _ga=GA1.2.181181747.1630349859;" +
                " __tld__=null;" +
                " dd__lastEventTimestamp=1630350353734;" +
                " cto_bundle=U_rsk185bk9iWDlyNExvQVpkQkw0VlFSSmFkRVVZSVVuWlFhY2xSQlU3aWVDRUQ4QlBEeXZFc1ZWTiUyRllP" +
                "MTVwaGtncDVxaGlrTXJQdFJPeXVHeHBtNjBjbUtKdkZUbUQ3SlU2Zk9tMFQxeE4lMkJYRXFHJTJCUjdtM25wUHF1WFBDWSUy" +
                "QmFGNG5TWEJ4YVRnalI3QXBhbyUyRmVlVE5mVFR3JTNEJTNE; mindboxDeviceUUID=567e1fb1-2b02-4011-a0b2-0ec6" +
                "c1e59d56;" +
                " directCrm-session={\"deviceGuid\":\"567e1fb1-2b02-4011-a0b2-0ec6c1e59d56\"};" +
                " _ga_XW7S332S1N=GS1.1.1630349858.1.1.1630350355.0; tmr_reqNum=16";
    }
}
