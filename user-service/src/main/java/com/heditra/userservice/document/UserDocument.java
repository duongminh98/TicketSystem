package com.heditra.userservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String username;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String email;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String firstName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String lastName;

    @Field(type = FieldType.Keyword)
    private String role;
}
