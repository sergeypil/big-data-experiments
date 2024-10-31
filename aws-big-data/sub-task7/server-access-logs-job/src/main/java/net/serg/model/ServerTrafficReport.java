package net.serg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerTrafficReport {
    private  String id;
    private  String source;
    private String target;
    private long totalRequests;
}
