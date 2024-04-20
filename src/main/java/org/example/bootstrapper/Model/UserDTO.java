package org.example.bootstrapper.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.bootstrapper.Enum.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String username;
    private Role role;

    public UserDTO(User user) {
        this.username = user.getUsername();
        this.role = user.getRole();
    }
}
