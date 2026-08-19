package org.acme.user;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GetUsersResponse {

  private List<User> users = new ArrayList<>();
}
