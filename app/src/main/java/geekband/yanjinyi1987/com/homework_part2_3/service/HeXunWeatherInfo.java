package geekband.yanjinyi1987.com.homework_part2_3.service;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by lexkde on 16-9-11.
 */
public class HeXunWeatherInfo {
    @SerializedName("HeWeather data service 3.0")
    public List<HeWeatherDS0300> heWeatherDS0300;

    public static class HeWeatherDS0300 {
        public Aqi aqi;
        public Basic basic;
        public List<DailyForecast> daily_forecase;
        public List<HourlyForecast> hourly_forecase;
        public Now now;
        public String status;
        public Suggestion suggestion;

        public static class Aqi {
            public City city;
            public static class City{
                public String aqi;
                public String co;
                public String no2;
                public String o3;
                public String pm10;
                public String pm25;
                public String qlty;
                public String so2;
            }
        }

        public static class Basic {
            public String city;
            public String cnty;
            public String id;
            public Float lat;
            public Float lon;
            public Update update;
            public static class Update {
                public String loc;
                public String utc;
            }
        }

        public static class DailyForecast {
            public Astro astro;
            public Cond cond;
            public String date;
            public int hum;
            public Float pcpn;
            public int pop;
            public int pres;
            public Temperature tmp;
            public int vis;
            public Wind wind;

            public static class Astro {
                public String sr;
                public String ss;
            }

            public static class Cond {
                public int code_d;
                public int code_n;
                public String txt_d;
                public String txt_n;
            }

            public static class Temperature {
                public int max;
                public int min;
            }
            public static class Wind {
                public int deg;
                public String dir;
                public String sc;
                public int spd;
            }
        }
        public static class HourlyForecast {
            public String date;
            public int hum;
            public int pop;
            public int pres;
            public int tmp;
            public Wind wind;
            public static class Wind {
                public int deg;
                public String dir;
                public String sc;
                public int spd;
            }
        }

        public static class Now {
            public Cond cond;
            public int fl;
            public int hum;
            public Float pcpn;
            public int pres;
            public int tmp;
            public int vis;
            Wind wind;
            public static class Cond {
                public int code;
                public String txt;
            }
            public static class Wind {
                public int deg;
                public String dir;
                public String sc;
                public int spd;
            }
        }
        public static class Suggestion {
            public Comf comf;
            public Cw cw;
            public Drsg drsg;
            public Flu flu;
            public Sport sport;
            public Trav trav;
            public UV uv;

            public static class Comf{
                public String brf;
                public String txt;
            }

            public static class Cw{
                public String brf;
                public String txt;
            }

            public static class Drsg{
                public String brf;
                public String txt;
            }

            public static class Flu{
                public String brf;
                public String txt;
            }

            public static class Sport{
                public String brf;
                public String txt;
            }

            public static class Trav{
                public String brf;
                public String txt;
            }

            public static class UV{
                public String brf;
                public String txt;
            }
        }
    }
}
