package com.example.RealTimeNews.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@Slf4j
public class NewsController {

    public List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    public Map<String, SseEmitter> mapEmitters = new ConcurrentHashMap<>();
    // method for client subscription
    @CrossOrigin
    @GetMapping(value = "/subscribe", consumes = MediaType.ALL_VALUE)
    public SseEmitter subscribe(@RequestParam String userId) {
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        sendInitEvent(sseEmitter);

        logFormListEmitters(sseEmitter);
        logFormMapEmitters(userId, sseEmitter);

        return sseEmitter;
    }

    // method for dispatching events to all clients
    @CrossOrigin
    @PostMapping(value = "/dispatchEventToAll")
    public void dispatchEventToClients(@RequestParam String title, @RequestParam String text){
        Map<String, String> mapForJson = Map.of("Title", title, "Text", text);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("latestNews").data(mapForJson));
                log.info("PostMapping for all clients SSE Emitters : {}", emitters);
            } catch (IOException e) {
                emitters.remove(emitter);
                log.info("Exception SSE Emitters : {}", emitters);
            }
        }
    }

    // method for dispatching events to specific clients
    @CrossOrigin
    @PostMapping(value = "/dispatchEventToSpecific")
    public void dispatchEventToSpecificClients(@RequestParam String title, @RequestParam String text, @RequestParam String userId){
        Map<String, String> mapForJson = Map.of("Title", title, "Text", text);
        SseEmitter sseEmitter = mapEmitters.get(userId);
        if (sseEmitter != null) {
            try {
                sseEmitter.send(SseEmitter.event().name("latestNews").data(mapForJson));
                log.info("PostMapping for specific clients SSE Emitters : {}", mapEmitters);
            } catch (IOException e) {
//                emitters.remove(sseEmitter);
                mapEmitters.remove(userId);
                log.info("Exception SSE Emitters : {}", mapEmitters);
            }
        }
    }

    private void sendInitEvent(SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event().name("INIT"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logFormListEmitters(SseEmitter sseEmitter) {
        log.info("first SSE Emitters : {}", emitters);

        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        log.info("onCompletion remove SSE Emitters : {}", emitters);

        emitters.add(sseEmitter);
        log.info("after add SSE Emitters : {}", emitters);
    }

    private void logFormMapEmitters(String userId, SseEmitter sseEmitter) {
        log.info("first SSE Emitters : {}", mapEmitters);
        mapEmitters.put(userId, sseEmitter);
        log.info("map SSE Emitters : {}", mapEmitters);


        sseEmitter.onCompletion(() -> mapEmitters.remove(userId));
        log.info("onCompletion remove SSE Emitters : {}", mapEmitters);

        sseEmitter.onTimeout(() -> mapEmitters.remove(userId));
        log.info("onTimeout remove SSE Emitters : {}", mapEmitters);

        sseEmitter.onError((e) -> mapEmitters.remove(userId));
        log.info("onError remove SSE Emitters : {}", mapEmitters);
    }
}
