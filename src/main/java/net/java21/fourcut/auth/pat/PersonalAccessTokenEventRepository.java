package net.java21.fourcut.auth.pat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalAccessTokenEventRepository
    extends JpaRepository<PersonalAccessTokenEvent, Long> {

  List<PersonalAccessTokenEvent> findByTokenIdOrderByIdAsc(Long tokenId);
}
