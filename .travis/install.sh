#!/usr/bin/env sh

if [ "$TRAVIS_BRANCH" = "master" ] && [ "$TRAVIS_PULL_REQUEST" = "false" ];
then
    openssl aes-256-cbc -K $encrypted_eb14e710fc65_key -iv $encrypted_eb14e710fc65_iv -in .travis/secret.gpg.enc -out $GPG_KEY_LOCATION -d
fi
