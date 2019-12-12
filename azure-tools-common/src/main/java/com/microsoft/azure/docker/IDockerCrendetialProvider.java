package com.microsoft.azure.docker;

public class IDockerCrendetialProvider {

    /**
     *
     *
     *             The username used to authenticate.
     *
     *
     */
    private String username;

    /**
     *
     *
     *             The password used in conjunction with the
     * username to authenticate.
     *
     *
     */
    private String password;

    public String getPassword()
    {
        return this.password;
    } //-- String getPassword()

    /**
     * Get the username used to authenticate.
     *
     * @return String
     */
    public String getUsername()
    {
        return this.username;
    } //-- String getUsername()

}
