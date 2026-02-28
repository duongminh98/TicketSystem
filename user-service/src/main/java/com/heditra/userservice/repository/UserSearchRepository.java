package com.heditra.userservice.repository;

import com.heditra.userservice.document.UserDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, String> {

    List<UserDocument> findByUsernameContaining(String username);

    List<UserDocument> findByEmailContaining(String email);

    List<UserDocument> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);

    List<UserDocument> findByRole(String role);
}
