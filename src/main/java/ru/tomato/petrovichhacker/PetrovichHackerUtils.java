package ru.tomato.petrovichhacker;

public class PetrovichHackerUtils {
    private static final String GET_PETROVICH_GOOD_URL_FORMAT = "https://petrovich.ru/catalog/6619/%s/";
    private static final String GET_PETROVICH_GOOD_REQUEST_PARAM_FORMAT = "?current_city=%D0%A1%D0%B0%D0%BD%D0%BA%D1%82-%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3";

    public static String getUrl(String goodId) {
        return String.format(GET_PETROVICH_GOOD_URL_FORMAT, goodId) + GET_PETROVICH_GOOD_REQUEST_PARAM_FORMAT;
    }
}
