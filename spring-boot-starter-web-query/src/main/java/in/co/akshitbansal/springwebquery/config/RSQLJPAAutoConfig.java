package in.co.akshitbansal.springwebquery.config;

import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

@AutoConfiguration
public class RSQLJPAAutoConfig {

    // Allows RSQL to parse ISO-8601 Timestamp fields
    // eg. 2025-12-08T00:00:00+00:00 or 2025-12-08T00:00:00Z
    // query eg. to query by createTimestamp on 2025-12-08 in UTC
    // use createTimestamp>=2025-12-08T00:00:00%2B00:00;createTimestamp<2025-12-09T00:00:00%2B00:00
    // %2B is URL encoding for +
    @PostConstruct
    public void init() {
        RSQLJPASupport.addConverter(Timestamp.class, value -> {
            OffsetDateTime odt = OffsetDateTime.parse(value);
            Instant instant = odt.toInstant();
            return Timestamp.from(instant);
        });
    }
}
