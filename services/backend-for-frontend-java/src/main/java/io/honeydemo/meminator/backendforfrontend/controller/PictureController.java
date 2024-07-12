package io.honeydemo.meminator.backendforfrontend.controller;

import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

@RestController
public class PictureController {

    private final WebClient phraseClient;
    private final WebClient imageClient;
    private WebClient memeClient;
    private static final Logger logger = LogManager.getLogger("backendForFrontend");

    @Autowired
    public PictureController(WebClient.Builder webClientBuilder) {
        this.imageClient = webClientBuilder.baseUrl("http://image-picker:10116").build();
        this.memeClient = webClientBuilder.baseUrl("http://meminator:10117").build();
        this.phraseClient = webClientBuilder.baseUrl("http://phrase-picker:10118").build();
    }

    @PostMapping("/createPicture")
    public Mono<Object> createPicture() throws MalformedURLException {

        var phraseResult = phraseClient.get().uri("/phrase").retrieve().toEntity(PhraseResult.class);
        var imageResult = imageClient.get().uri("/imageUrl").retrieve().toEntity(ImageResult.class);

        var bothResults = Mono.zip(phraseResult, imageResult);

        // Set content type header
        MediaType mediaType = MediaType.IMAGE_PNG;
        logger.info("media type is " + mediaType);

        var meme = bothResults.flatMap(v -> {
            String phrase = v.getT1().getBody().getPhrase();
            String imageUrl = v.getT2().getBody().getImageUrl();
            logger.info("app.phrase=" + phrase + ", app.imageUrl=" + imageUrl);

            return memeClient.post().uri("/applyPhraseToPicture").bodyValue(new MemeRequest(phrase, imageUrl))
                    .retrieve().toEntity(byte[].class);
        });

        // Return the image file as a ResponseEntity
        return meme.map(v -> {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(v.getBody());
        });
    }

    static class PhraseResult {
        private String phrase;

        public PhraseResult() {
        }

        public String getPhrase() {
            return phrase;
        }

        public void setPhrase(String phrase) {
            this.phrase = phrase;
        }
    }

    static class ImageResult {
        private String imageUrl;

        public ImageResult() {
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String phrase) {
            this.imageUrl = phrase;
        }

    }

    static class MemeRequest {
        private String phrase;
        private String imageUrl;

        public MemeRequest() {
        }

        public MemeRequest(String phrase, String imageUrl) {
            this.phrase = phrase;
            this.imageUrl = imageUrl;
        }

        public String getPhrase() {
            return phrase;
        }

        public void setPhrase(String phrase) {
            this.phrase = phrase;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
