package maestro.orchestra.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.truth.Truth.assertThat
import maestro.orchestra.MaestroCommand
import maestro.orchestra.RepeatCommand
import maestro.orchestra.RetryCommand
import maestro.orchestra.RunFlowCommand
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class SourceInfoTest {

    @Test
    fun `attaches sourceInfo to top-level commands`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- launchApp
            |- tapOn:
            |    text: Login
            |- inputText: hello
            |- back
        """.trimMargin("|")

        val commands = MaestroFlowParser.parseFlow(Paths.get("/spike/flow.yaml"), yaml)
        val userCommands = commands.drop(1) // drop synthetic applyConfig

        val launch = userCommands[0].sourceInfo!!
        assertThat(launch.startLine).isEqualTo(3)
        assertThat(launch.endLine).isEqualTo(3)
        assertThat(launch.path).isEqualTo("/spike/flow.yaml")
        assertThat(launch.source).isEqualTo(yaml)

        val tap = userCommands[1].sourceInfo!!
        assertThat(tap.startLine).isEqualTo(4)
        assertThat(tap.endLine).isEqualTo(5)
        assertThat(tap.source.substring(tap.startOffset, tap.endOffset))
            .isEqualTo("tapOn:\n    text: Login")

        val input = userCommands[2].sourceInfo!!
        assertThat(input.startLine).isEqualTo(6)
        assertThat(input.endLine).isEqualTo(6)

        val back = userCommands[3].sourceInfo!!
        assertThat(back.startLine).isEqualTo(7)
        assertThat(back.endLine).isEqualTo(7)
    }

    @Test
    fun `equality ignores sourceInfo`() {
        val yaml1 = "appId: com.example.app\n---\n- inputText: hello\n"
        val yaml2 = "appId: com.example.app\n---\n\n\n- inputText: hello\n"
        val a = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml1).last()
        val b = MaestroFlowParser.parseFlow(Paths.get("/b.yaml"), yaml2).last()
        assertThat(a.sourceInfo!!.startLine).isNotEqualTo(b.sourceInfo!!.startLine)
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `offsets agree with line and column`() {
        val yaml = "appId: com.example.app\n---\n- inputText: hello\n"
        val cmd = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml).last()
        val s = cmd.sourceInfo!!
        // startOffset points at startColumn within startLine — Jackson reports the value start ('i').
        assertThat(s.source[s.startOffset]).isEqualTo('i')
        // endOffset is one past the last char of endLine.
        assertThat(s.source.substring(0, s.endOffset)).endsWith("- inputText: hello")
    }

    // The deserializer must use ctxt.readValue (not parser.codec.readValue) so that
    // the parse-context attribute propagates into nested YamlFluentCommand lists.
    // Without that, inner commands either lose source info or fail outright.

    @Test
    fun `propagates sourceInfo into repeat block commands`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- repeat:
            |    times: 2
            |    commands:
            |      - tapOn: foo
            |      - back
        """.trimMargin("|")

        val repeat = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml)
            .mapNotNull { it.asCommand() as? RepeatCommand }
            .single()

        val tap = repeat.commands[0].sourceInfo!!
        assertThat(tap.startLine).isEqualTo(6)
        assertThat(tap.source).isEqualTo(yaml)

        val back = repeat.commands[1].sourceInfo!!
        assertThat(back.startLine).isEqualTo(7)
        assertThat(back.source).isSameInstanceAs(tap.source) // shared reference, not duplicated
    }

    @Test
    fun `propagates sourceInfo into runFlow inline commands`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- runFlow:
            |    commands:
            |      - assertTrue: ${'$'}{1 == 1}
            |      - back
        """.trimMargin("|")

        val runFlow = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml)
            .mapNotNull { it.asCommand() as? RunFlowCommand }
            .single()

        assertThat(runFlow.commands[0].sourceInfo!!.startLine).isEqualTo(5)
        assertThat(runFlow.commands[1].sourceInfo!!.startLine).isEqualTo(6)
    }

    @Test
    fun `propagates sourceInfo into retry block commands`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- retry:
            |    maxRetries: 3
            |    commands:
            |      - tapOn: foo
            |      - back
        """.trimMargin("|")

        val retry = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml)
            .mapNotNull { it.asCommand() as? RetryCommand }
            .single()

        assertThat(retry.commands[0].sourceInfo!!.startLine).isEqualTo(6)
        assertThat(retry.commands[1].sourceInfo!!.startLine).isEqualTo(7)
    }

    // Trim heuristic: end-line walk-back is YAML-shape guesswork. The cases below
    // are most likely to silently regress if Jackson reports different end positions.

    @Test
    fun `multi-line command at end of file has correct endLine`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- tapOn:
            |    text: Login
        """.trimMargin("|")

        val cmd = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml).last()
        val s = cmd.sourceInfo!!
        assertThat(s.startLine).isEqualTo(3)
        assertThat(s.endLine).isEqualTo(4)
    }

    @Test
    fun `last command inside a repeat block has correct endLine`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- repeat:
            |    times: 2
            |    commands:
            |      - tapOn:
            |          text: Login
            |- back
        """.trimMargin("|")

        val repeat = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml)
            .mapNotNull { it.asCommand() as? RepeatCommand }
            .single()
        val tap = repeat.commands.single().sourceInfo!!
        assertThat(tap.startLine).isEqualTo(6)
        assertThat(tap.endLine).isEqualTo(7)
    }

    @Test
    fun `block scalar value containing a # line is not trimmed`() {
        // A '#' line inside a YAML block scalar is part of the value, not a YAML
        // comment. The trim heuristic must only trim outer-indent comments.
        val yaml = """
            |appId: com.example.app
            |---
            |- evalScript: |
            |    let x = 1
            |    # this is a JS comment, part of the script
            |- back
        """.trimMargin("|")

        val commands = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml)
        val script = commands[1].sourceInfo!!
        assertThat(script.startLine).isEqualTo(3)
        assertThat(script.endLine).isEqualTo(5)
    }

    @Test
    fun `command followed by trailing comment keeps endLine on the command`() {
        val yaml = """
            |appId: com.example.app
            |---
            |- tapOn:
            |    text: Login
            |# trailing comment
            |- back
        """.trimMargin("|")

        val commands = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml)
        val tap = commands[1].sourceInfo!!
        assertThat(tap.startLine).isEqualTo(3)
        assertThat(tap.endLine).isEqualTo(4)
    }

    @Test
    fun `sourceInfo is dropped on json round-trip`() {
        val yaml = "appId: com.example.app\n---\n- inputText: hello\n"
        val parsed = MaestroFlowParser.parseFlow(Paths.get("/a.yaml"), yaml).last()
        assertThat(parsed.sourceInfo).isNotNull()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val json = mapper.writeValueAsString(parsed)
        val roundTripped = mapper.readValue(json, MaestroCommand::class.java)

        assertThat(roundTripped.sourceInfo).isNull()
        assertThat(roundTripped).isEqualTo(parsed) // equality ignores sourceInfo
        assertThat(json).doesNotContain("sourceInfo")
        assertThat(json).doesNotContain(yaml) // origin source must not be serialized
    }
}
