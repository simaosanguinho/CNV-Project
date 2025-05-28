{

  description = "Development shell for CNV Project";
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let pkgs = import nixpkgs { inherit system; };
      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs; [ jdk17 maven jq zip];
          shellHook = ''
            export JAVA_HOME=${pkgs.jdk17}
            PATH="${pkgs.jdk17}/bin:$PATH"
          '';
        };
      });
}
