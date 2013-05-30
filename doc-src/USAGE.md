## Usage

The docker crate provides a `server-spec` function that returns a
server-spec. This server spec will install and run the docker daemon.

The `server-spec` provides an easy way of using the crate functions, and you can
use the following crate functions directly if you need to.

The `settings` function provides a plan function that should be called in the
`:settings` phase.  The function puts the configuration options into the pallet
session, where they can be found by the other crate functions, or by other
crates wanting to interact with docker.

The `install` function is responsible for actually installing docker.  At
present installation from ppa is the only supported method.

Only supports Ubuntu 12.04 and 13.04.
