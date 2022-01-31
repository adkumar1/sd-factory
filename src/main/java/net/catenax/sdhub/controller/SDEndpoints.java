package net.catenax.sdhub.controller;

import com.danubetech.verifiablecredentials.VerifiablePresentation;
import com.mongodb.DBObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.catenax.sdhub.service.VerifiableCredentialService;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("selfdescription")
@RequiredArgsConstructor
@Slf4j
public class SDEndpoints {

    private final VerifiableCredentialService verifiableCredentialService;
    private final MongoTemplate mongoTemplate;

    @PostMapping(consumes = {"application/did+ld+json"})
    public void publishSelfDescription(@RequestBody VerifiablePresentation verifiablePresentation) throws Exception{
        var verifier = verifiableCredentialService.createVerifier(verifiablePresentation);
        if (verifier.verifier().verify(verifiablePresentation)) {
            log.debug("Verifiable Presentation is authentic for controller {}", verifier.controller());
            var vc = verifiablePresentation.getVerifiableCredential();
            verifier = verifiableCredentialService.createVerifier(vc);
            if (verifier.verifier().verify(vc)) {
                log.debug("Verifiable Credential is authentic for controller {}", verifier.controller());
                Document doc = Document.parse(verifiablePresentation.toJson());
                mongoTemplate.save(doc, "selfdescription");
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-Description is not authentic");
    }

    @GetMapping(value = "/id/{id}", produces = {"application/did+ld+json"})
    public DBObject getById( @PathVariable("id") String id) {
        var query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        return Optional.ofNullable(mongoTemplate.findOne(query, DBObject.class, "selfdescription"))
                .stream()
                .peek(dbObj -> dbObj.removeField("_id"))
                .findAny().orElse(null);
    }
}