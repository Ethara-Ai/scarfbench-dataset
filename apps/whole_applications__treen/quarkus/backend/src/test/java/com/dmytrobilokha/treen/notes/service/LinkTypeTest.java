package com.dmytrobilokha.treen.notes.service;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LinkTypeTest {

    @DataProvider(name = "linksData")
    public Object[][] getLinksData() {
        return new Object[][]{
                {"www.aaa.com", LinkType.WEB, "https://www.aaa.com"},
                {"www.aaa9.com", LinkType.WEB, "https://www.aaa9.com"},
                {"www.aa-a.com", LinkType.WEB, "https://www.aa-a.com"},
                {"aa-a.com", LinkType.WEB, "https://aa-a.com"},
                {"www.aaa.", LinkType.UNKNOWN, "www.aaa."},
                {"http://www.aaa.com", LinkType.WEB, "http://www.aaa.com"},
                {"https://www.aaa.com", LinkType.WEB, "https://www.aaa.com"},
                {"xhttps://www.aaa.com", LinkType.UNKNOWN, "xhttps://www.aaa.com"},
                {" www.aaa.com", LinkType.WEB, "https://www.aaa.com"},
                {"geo:55.3334252,3.223145", LinkType.GEO, "geo:55.3334252,3.223145"},
                {"geo:55.3334252, 3.223145", LinkType.GEO, "geo:55.3334252,3.223145"},
                {"geo:  55.3334252, 3.223145", LinkType.GEO, "geo:55.3334252,3.223145"},
                {"55.3334252,3.223145", LinkType.GEO, "geo:55.3334252,3.223145"},
                {"55.3334252, 3.223145", LinkType.GEO, "geo:55.3334252,3.223145"},
                {"55.33xxx34252,3.223145", LinkType.UNKNOWN, "55.33xxx34252,3.223145"},
                {"51.830022258409905, 5.846889327615317", LinkType.GEO, "geo:51.830022258409905,5.846889327615317"},
                {"43.74774068304437, -79.44500725430373", LinkType.GEO, "geo:43.74774068304437,-79.44500725430373"},
                {"34.64868695772392, 135.55523002973584", LinkType.GEO, "geo:34.64868695772392,135.55523002973584"},
                {"-27.090143756977422, 134.8668992349146", LinkType.GEO, "geo:-27.090143756977422,134.8668992349146"},
                {"-15.97713505750436, -47.24042222632914", LinkType.GEO, "geo:-15.97713505750436,-47.24042222632914"},
        };
    }

    @Test(dataProvider = "linksData")
    public void detectsLinkType(String link, LinkType type, String formatted) {
        Assert.assertEquals(LinkType.detect(link), type);
    }

    @Test(dataProvider = "linksData")
    public void formatsLink(String link, LinkType type, String formatted) {
        Assert.assertEquals(type.format(link), formatted);
    }

}
