// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20; // Specifies the compiler version

/**
 * @title HelloWorld
 * @dev A basic contract that returns "Hello World!".
 */
contract HelloWorld {

    /**
     * @dev Returns the classic "Hello World!" string.
     * @return A string containing "Hello World!".
     */
    function sayHello() public pure returns (string memory) {
        return "Hello World!";
    }

}