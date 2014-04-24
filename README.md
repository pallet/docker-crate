[Repository](https://github.com/pallet/docker-crate) &#xb7;
[Issues](https://github.com/pallet/docker-crate/issues) &#xb7;
[API docs](http://palletops.com/docker-crate/0.8/api) &#xb7;
[Annotated source](http://palletops.com/docker-crate/0.8/annotated/uberdoc.html) &#xb7;
[Release Notes](https://github.com/pallet/docker-crate/blob/develop/ReleaseNotes.md)

A [pallet](http://palletops.com/) crate to install and configure
 [docker](http://docker.io).

### Dependency Information

```clj
:dependencies [[com.palletops/docker-crate "0.8.0-alpha.2"]]
```

### Releases

<table>
<thead>
  <tr><th>Pallet</th><th>Crate Version</th><th>Repo</th><th>GroupId</th></tr>
</thead>
<tbody>
  <tr>
    <th>0.8.0-RC.1</th>
    <td>0.8.0-alpha.2</td>
    <td>clojars</td>
    <td>com.palletops</td>
    <td><a href='https://github.com/pallet/docker-crate/blob/0.8.0-alpha.2/ReleaseNotes.md'>Release Notes</a></td>
    <td><a href='https://github.com/pallet/docker-crate/blob/0.8.0-alpha.2/'>Source</a></td>
  </tr>
</tbody>
</table>

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

The `run` function can be used to start a container, `kill` to stop a container.

The `nodes` function lists running containers.

Only supports Ubuntu 12.04 and 13.04 as the docker host node.

## License

Copyright (C) 2013 Hugo Duncan

Distributed under the Eclipse Public License.
