package com.banka1.banking.models.helper;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
@Getter
public enum BusinessActivityCode {

    COMPUTER_PROGRAMMING("6201", "Computer programming"),
    RETAIL_SALES("4711", "Retail sale in non-specialized stores with food, beverages, or tobacco predominating"),
    REAL_ESTATE_RENTAL("6820", "Renting and operating of own or leased real estate"),
    RESTAURANTS("5610", "Restaurants and mobile food service activities"),
    TAXI_SERVICE("4932", "Taxi operation"),
    BANK("1111", "Bank"),
    COUNTRY("2222", "Country"),
    CROP_CULTIVATION("1.11", "Cultivation of cereals and legumes"),
    VEGETABLE_GROWING("1.13", "Vegetable growing"),
    TEXTILE_PREPARATION("13.1", "Preparation and spinning of textile fibers"),
    IRON_STEEL_PRODUCTION("24.1", "Production of iron and steel"),
    STEEL_PIPES_PRODUCTION("24.2", "Production of steel pipes, hollow profiles and fittings"),
    CONSTRUCTION_PROJECT_DEVELOPMENT("41.1", "Development of construction projects"),
    BUILDING_CONSTRUCTION("41.2", "Construction of residential and non-residential buildings"),
    ROAD_CONSTRUCTION("42.11", "Construction of roads and highways"),
    RAILWAY_CONSTRUCTION("42.12", "Construction of railways and underground railways"),
    BRIDGE_TUNNEL_CONSTRUCTION("42.13", "Construction of bridges and tunnels"),
    WATER_PROJECT_CONSTRUCTION("42.21", "Construction of water projects"),
    UTILITY_NETWORK_CONSTRUCTION("42.22", "Construction of electricity and telecommunication networks"),
    COAL_MINING("5.1", "Coal mining"),
    IRON_ORE_MINING("7.1", "Mining of iron ores"),
    URANIUM_THORIUM_MINING("7.21", "Mining of uranium and thorium"),
    STONE_QUARRYING("8.11", "Quarrying of ornamental and building stone"),
    PEAT_EXTRACTION("8.92", "Extraction of peat"),
    RETAIL_FOOD_STORES("47.11", "Retail sale in non-specialized stores with food and beverages"),
    RESTAURANT_ACTIVITIES("56.1", "Restaurants and mobile food service activities"),
    OTHER_IT_SERVICES("62.09", "Other IT services"),
    DATA_PROCESSING("63.11", "Data processing, hosting and related activities"),
    MONETARY_INTERMEDIATION("64.19", "Other monetary intermediation activities"),
    FINANCIAL_LEASING("64.91", "Financial leasing"),
    HOLDING_COMPANIES("64.2", "Activities of holding companies"),
    FUNDS_FINANCIAL_ENTITIES("66.3", "Funds and similar financial entities"),
    REINSURANCE("65.2", "Reinsurance"),
    LIFE_INSURANCE("65.11", "Life insurance"),
    NON_LIFE_INSURANCE("65.12", "Non-life insurance"),
    RISK_DAMAGE_ASSESSMENT("66.21", "Risk and damage assessment"),
    REAL_ESTATE_MANAGEMENT("68.1", "Real estate management on a fee or contract basis"),
    POSTAL_ACTIVITIES("53.1", "Postal activities"),
    COURIER_ACTIVITIES("53.2", "Courier activities"),
    PRESCHOOL_EDUCATION("85.1", "Pre-primary education"),
    PRIMARY_EDUCATION("85.2", "Primary education"),
    HOSPITAL_ACTIVITIES("86.1", "Hospital activities"),
    GENERAL_MEDICAL_PRACTICE("86.21", "General medical practice activities"),
    SPECIALIST_MEDICAL_PRACTICE("86.22", "Specialist medical practice activities"),
    OTHER_HEALTHCARE("86.9", "Other human health activities"),
    ECONOMIC_REGULATION("84.12", "Regulation of economic activities"),
    THEATER_ACTIVITIES("90.01", "Performing arts activities"),
    MUSEUM_ACTIVITIES("90.02", "Museum activities"),
    BOTANICAL_ZOO_GARDENS("90.04", "Botanical and zoological gardens activities"),
    SPORTS_FACILITIES("93.11", "Operation of sports facilities"),
    FITNESS_FACILITIES("93.13", "Fitness facilities"),
    OTHER_SPORTS_ACTIVITIES("93.19", "Other sports activities"),
    ELECTRONIC_COMPONENTS("26.11", "Manufacture of electronic components"),
    ELECTRICAL_EQUIPMENT("27.12", "Manufacture of electricity distribution and control equipment"),
    MOTOR_VEHICLE_MANUFACTURING("29.1", "Manufacture of motor vehicles");

    private final String code;
    private final String description;

    BusinessActivityCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static BusinessActivityCode fromCode(String code) {
        for (BusinessActivityCode activity : values()) {
            if (activity.getCode().equals(code)) {
                return activity;
            }
        }
        throw new IllegalArgumentException("Invalid activity code: " + code);
    }

    public static List<String> getAll() {
        List<String> codes = new ArrayList<>();
        for (BusinessActivityCode activity : values()) {
            codes.add(String.format("%s -> %s", activity.getCode(), activity.getDescription()));
        }
        System.out.println(codes);
        return codes;
    }
}

