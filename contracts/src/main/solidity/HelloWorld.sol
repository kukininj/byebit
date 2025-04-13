// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.7.0;

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
