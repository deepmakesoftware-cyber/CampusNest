package com.campusnest.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.campusnest.listing.Amenity;
import com.campusnest.listing.AmenityRepository;
import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.ObjectMapper;

/** Helpers that build listings step by step, so each workflow test reads like a story. */
public abstract class ListingTestSupport extends ApiTestSupport {

    protected static final String OWNER_LISTINGS = "/api/v1/owner/listings";
    protected static final String ADMIN_LISTINGS = "/api/v1/admin/listings";

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AmenityRepository amenities;

    protected long createDraft(MockHttpSession owner) throws Exception {
        String body = mockMvc.perform(post(OWNER_LISTINGS).with(csrf()).session(owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sunrise PG Matunga\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** A complete, realistic listing body near VJTI. Tests copy and tweak it. */
    protected Map<String, Object> completeListing() {
        Map<String, Object> room = new LinkedHashMap<>();
        room.put("roomType", "DOUBLE");
        room.put("sharingCapacity", 2);
        room.put("monthlyRent", 9500);
        room.put("securityDeposit", 19000);
        room.put("bedsTotal", 6);
        room.put("bedsAvailable", 2);

        Map<String, Object> charge = Map.of("chargeType", "ELECTRICITY", "amount", 800, "frequency", "MONTHLY",
                "mandatory", true);

        List<Map<String, Object>> answers = new ArrayList<>();
        for (Amenity a : amenities.findAllByOrderBySortOrderAsc()) {
            if (a.isCore()) {
                answers.add(Map.of("amenityId", a.getId(), "available", !a.getCode().equals("AC")));
            }
        }

        Map<String, Object> listing = new LinkedHashMap<>();
        listing.put("title", "Sunrise PG Matunga");
        listing.put("description", "Clean double-sharing rooms, 10 minutes walk from VJTI main gate. Home-cooked meals.");
        listing.put("propertyType", "PG");
        listing.put("genderPolicy", "ANY");
        listing.put("addressLine", "12, Example Lane, Near Five Gardens");
        listing.put("locality", "Matunga East");
        listing.put("pincode", "400019");
        listing.put("latitude", 19.0251);
        listing.put("longitude", 72.8559);
        listing.put("rooms", new ArrayList<>(List.of(room)));
        listing.put("charges", List.of(charge));
        listing.put("amenities", answers);
        return listing;
    }

    protected ResultActions putListing(MockHttpSession owner, long id, Map<String, Object> listing) throws Exception {
        return mockMvc.perform(put(OWNER_LISTINGS + "/" + id).with(csrf()).session(owner)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(listing)));
    }

    protected static byte[] pngBytes() throws IOException {
        BufferedImage img = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    protected ResultActions uploadImage(MockHttpSession owner, long id, byte[] bytes, String filename) throws Exception {
        return mockMvc.perform(multipart(OWNER_LISTINGS + "/" + id + "/images")
                .file(new MockMultipartFile("file", filename, "image/png", bytes))
                .with(csrf()).session(owner));
    }

    /** Returns the id of the first photo of a listing (from the owner's view). */
    protected long firstImageId(MockHttpSession owner, long listingId) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get(OWNER_LISTINGS + "/" + listingId).session(owner))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.images[0].id")).longValue();
    }

    /** Draft -> complete -> photo -> submitted. Returns the listing id (status PENDING_VERIFICATION). */
    protected long submittedListing(MockHttpSession owner) throws Exception {
        long id = createDraft(owner);
        putListing(owner, id, completeListing()).andExpect(status().isOk());
        uploadImage(owner, id, pngBytes(), "room.png").andExpect(status().isCreated());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner))
                .andExpect(status().isOk());
        return id;
    }

    /** Submitted and approved by an admin. Returns the listing id (status PUBLISHED_AVAILABLE). */
    protected long publishedListing(MockHttpSession owner, MockHttpSession admin) throws Exception {
        long id = submittedListing(owner);
        mockMvc.perform(post(ADMIN_LISTINGS + "/" + id + "/approve").with(csrf()).session(admin))
                .andExpect(status().isOk());
        return id;
    }

    /**
     * Publishes a listing whose title contains a unique token, so a test can find exactly its own listing with
     * the search text filter (?q=token) even though other tests' listings share the database.
     */
    protected long publishedWithToken(MockHttpSession owner, MockHttpSession admin, String token,
            java.util.function.Consumer<Map<String, Object>> customise) throws Exception {
        long id = createDraft(owner);
        Map<String, Object> listing = completeListing();
        listing.put("title", "Listing " + token);
        customise.accept(listing);
        putListing(owner, id, listing).andExpect(status().isOk());
        uploadImage(owner, id, pngBytes(), "room.png").andExpect(status().isCreated());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner)).andExpect(status().isOk());
        mockMvc.perform(post(ADMIN_LISTINGS + "/" + id + "/approve").with(csrf()).session(admin)).andExpect(status().isOk());
        return id;
    }

    protected static String token() {
        return "t" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    protected ResultActions adminAction(MockHttpSession admin, long id, String action, String reason) throws Exception {
        var request = post(ADMIN_LISTINGS + "/" + id + "/" + action).with(csrf()).session(admin);
        if (reason != null) {
            request = request.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("reason", reason)));
        }
        return mockMvc.perform(request);
    }
}
